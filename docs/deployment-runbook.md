# 远端部署 Runbook（仅供人工审核，不可自动执行）

> **状态：远程数据库 `<REMOTE_DB_HOST>` 当前冻结。**
> 本 Runbook 准备的是审核与将来执行的参考方案，文档本身 **不执行任何远端命令、不连接远端数据库**。
> 本文中所有远端地址、账号、密码、Token、JWT secret 均使用占位符，不包含任何真实凭证。

> **⚠️ 迁移路径（v2.0 生效）：**
> 远端实际迁移路径为 **v1.0 → v2.0**：仅 `CREATE TABLE IF NOT EXISTS` 三张新表（`patient` / `user_account` / `patient_register_link`）+ `INSERT...SELECT` 自 `register`，**不修改任何既有业务表**（不 ALTER / DROP / MODIFY `register` / `employee` / 处方 / 检验检查等任何旧表）。
>
> v1.1 / v1.2 已标记为 **SUPERSEDED，远端禁止执行**（会 ALTER 旧表 `register` / `employee`）。其升级顺序与回滚 SQL **已从本 Runbook 移除**，仅在下文以文字保留历史说明，不再提供可复制执行的旧 SQL。v2.0 失败策略见 §D.0。

---

## A. 部署前检查（人工）

```
1. 备份数据库
   mysqldump -h <REMOTE_DB_HOST> -u <REMOTE_DB_USER> -p<REMOTE_DB_PASSWORD> \
     --single-transaction --routines --triggers \
     hospital_cloud_brain > pre_migration_backup_YYYYMMDD.sql

2. 确认 MySQL 版本
   SELECT VERSION();  -- 期望 8.0.x

3. 确认当前仍为 v1.0（远端原基线，无身份/账号表）
   SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema='hospital_cloud_brain' AND table_name='patient';
   -- 期望 0（patient 表由 v2.0 创建，v2.0 执行前不应存在）

4. 确认无未审批的手工 schema 改动
   SHOW CREATE TABLE register \G
   -- 期望无 patient_id 列（v2.0 不给 register 加列）

5. 在隔离环境再次验证 migration
   在独立 Docker 容器中执行 v1.0 → v2.0 migration → v2.0 verify，
   全部通过后再进入远端执行。
   （历史路径 v1.1 → v1.2 已 SUPERSEDED，隔离环境也不再使用。）

6. 确认首个 ADMIN 的人工创建方案
   迁移后 user_account 初始为空；首个 ADMIN 必须由 DBA 通过 SQL 手工创建。
   占位符模板见下文 §F，本 Runbook 不含真实账号 / 密码 / BCrypt 哈希。
```

---

## B. 推荐升级顺序（当前生效：v1.0 → v2.0）

```text
1. 备份数据库（见 §A.1）

2. 执行 sql/v2.0_identity_schema_migration.sql
   mysql -h <REMOTE_DB_HOST> -u <REMOTE_DB_USER> -p<REMOTE_DB_PASSWORD> \
     hospital_cloud_brain < sql/v2.0_identity_schema_migration.sql
   该脚本仅 CREATE TABLE IF NOT EXISTS 三张新表 + INSERT...SELECT 自 register，
   不 ALTER/DROP/MODIFY 任何既有业务表；幂等可重跑
   （patient 与 link 均用 ON DUPLICATE KEY 不覆盖既有行，人工 link 不被覆盖）。

3. 执行 sql/v2.0_identity_schema_verify.sql（只读校验）
   必须通过：
     - patient / user_account / patient_register_link 三表存在
     - register 无 patient_id 列（零 ALTER 证明，期望 0）
     - employee 无 role / token_version 列（期望 0）
     - user_account 行数 = 0（初始无账号）
     - patient 无重复 card_number；link 无“一 register 多 patient”

4. 人工处理冲突 / 异常 card_number（v2.0 设计上不建 link，待人工）
   - 异常格式（NULL/空/长度非18/格式不符）→ 确认为历史数据或补正
   - 冲突组（同 card_number 下 real_name/gender/birthdate 不一致）
     → 人工选择归并目标 patient 后建立 MANUAL link

5. 人工创建首个 ADMIN 账号
   占位符模板见 §F。不要在本 Runbook 或任何代码库中填入真实凭证。

6. 部署应用代码
   - 编译：mvn -DskipTests clean package
   - 替换 JAR / Docker 镜像
   - 注入生产环境变量（JWT secret、DB 密码、Redis 密码，见 §E）

7. 三角色登录与权限冒烟（见 §C）
```

> 历史路径说明（v1.1 / v1.2，仅作记录，**不可执行**）：
> v1.1 通过 `ALTER TABLE register ADD COLUMN patient_id` 承载患者归属，v1.2 在 `employee`/`user_account` 上追加 `token_version` 列与唯一索引；二者均修改了既有业务表结构，与 v2.0“不修改旧业务表”的约束冲突，已废弃。其可执行 SQL 已从本 Runbook 移除，文件本体（`sql/v1.1_*` / `sql/v1.2_*`）仅保留 SUPERSEDED 头部标注供历史追溯。

---

## C. 上线后冒烟清单（人工逐项验证）

### C.1 登录与 JWT

| # | 用例 | 期望 |
|---|------|------|
| 1 | ADMIN 登录 | 200，`userInfo.role=ADMIN`，JWT 含 `tv` |
| 2 | DOCTOR 登录 | 200，`userInfo.role=DOCTOR`，`employeeId` 有值 |
| 3 | PATIENT 登录 | 200，`userInfo.role=PATIENT`，`patientId` 有值 |
| 4 | 错误密码 | 401 `账号或密码错误`（不泄露账户是否存在） |
| 5 | 禁用账号登录 | 401 |

### C.2 Token 失效

| # | 用例 | 期望 |
|---|------|------|
| 6 | ADMIN 禁用某账号后该账号已有 Token 请求 `/auth/me` | 401（tv 不匹配） |
| 7 | 禁用后重新启用，旧 Token 仍 | 401，新 Token 200 |
| 8 | 登录后自行改密码，改后旧 Token | 401 |
| 9 | ADMIN 重置某账号密码，旧 Token | 401，新密码可登录 |
| 10 | 登出后旧 Token | 401（黑名单） |

### C.3 权限隔离

| # | 用例 | 期望 |
|---|------|------|
| 11 | ADMIN 访问 `/api/v1/employee` | 200 |
| 12 | DOCTOR 访问 `/api/v1/employee` | **403** |
| 13 | PATIENT 访问 `/api/v1/register` | **403** |
| 14 | DOCTOR 查看本人接诊的 register | 200 |
| 15 | DOCTOR 查看其他医生接诊的 register | **404** |
| 16 | ADMIN 查看任意 register | 200 |
| 17 | DOCTOR 为本人 register 创建处方 | 200 |
| 18 | DOCTOR 为其他医生 register 创建处方 | **404** |

### C.4 患者门户

| # | 用例 | 期望 |
|---|------|------|
| 19 | PATIENT `/patient/profile` | 200，无 cardNumber/phone/homeAddress |
| 20 | PATIENT `/patient/records` | 仅返回本人就诊记录 |
| 21 | PATIENT `/patient/records/{他人registerId}` | **404** |

### C.5 账号管理

| # | 用例 | 期望 |
|---|------|------|
| 22 | ADMIN 创建 DOCTOR 账号绑定 employee | 200，response 不含 password |
| 23 | 同 employee 再次绑定 | **409** |
| 24 | ADMIN 创建 PATIENT 账号绑定 patient | 200，response 不含敏感字段 |
| 25 | 同 patient 再次绑定 | **409** |
| 26 | ADMIN 禁用非自己账号 | 200 |
| 27 | ADMIN 尝试禁用自己 | **409** |
| 28 | ADMIN 重置某账号密码 | 200，旧 Token 401，新密码可登录 |

### C.6 患者注册

| # | 用例 | 期望 |
|---|------|------|
| 29 | 新 cardNumber 注册 | 200，可登录，`/auth/me=PATIENT` |
| 30 | 已有 patient 资料一致注册 | 200，绑定，不修改 patient 身份 |
| 31 | 已有 patient 资料不一致注册 | 400 安全失败 |
| 32 | 重复 username 注册 | 400 安全失败 |

### C.7 日志安全

| # | 检查 |
|---|------|
| 33 | `grep -E "cardNumber|password|phone|homeAddress"` 应用日志，确认无敏感字段明文输出 |
| 34 | 确认 JWT secret、DB 密码未出现在日志中 |

---

## D. 回滚策略

### D.0 v2.0 迁移失败策略（当前生效）

> **绝对禁止 `DROP DATABASE` / 删库回滚。** v2.0 不修改任何既有业务表，失败时无需也不得删库。

v2.0 migration 失败时的处理顺序：

```
1. 立即停止执行，保留现场（不要重跑、不要删库、不要手动改表）。
2. 执行 sql/v2.0_identity_schema_verify.sql（只读），记录全部输出。
3. 根据 verify 输出定位失败点：
   - 若三张新表已建但 INSERT...SELECT 中途失败 → 新表数据可能不完整，
     但旧表（register 等）未被修改，业务可继续在旧 schema 上运行。
   - 若仅索引缺失或行数不符 → 由 DBA 评估后手工补建/补数。
4. 人工处理冲突 card_number、异常格式记录（v2.0 设计上不建 link，待人工）。
5. 如需回退到迁移前状态，唯一安全方式是：DBA 审核后 DROP 三张新表
   （patient / user_account / patient_register_link）—— 因 v2.0 未给旧表加任何外键/列，
   DROP 新表不影响旧业务表。此操作必须人工审批，且仅在确认无生产账号依赖新表时执行。
6. 任何情况下都不得执行 DROP DATABASE / DROP SCHEMA。
```

> v2.0 的真正“回滚”= 从 §A 步骤 1 的 `pre_migration_backup_YYYYMMDD.sql` 恢复，
> 但因 v2.0 不改旧表，通常无需整体恢复；优先采用“停止→verify→人工补正”。

### D.1 应用代码回滚

```
1. 恢复上一版 JAR / Docker 镜像。
2. v2.0 不修改任何既有业务表结构，旧应用代码与新 schema 在“旧业务表”维度完全兼容；
   若旧应用代码不识别三张新表，仅表现为身份/账号相关接口不可用，不影响既有业务流程。
3. 回滚前仍建议在隔离环境验证旧代码在 v2.0 schema 上能否正常启动。
```

### D.2 历史回滚路径（v1.1 / v1.2，已 SUPERSEDED，不提供可执行 SQL）

> v1.1 / v1.2 的 schema 回滚涉及 `DROP INDEX` / `DROP COLUMN` / `DROP TABLE` 等对既有业务表的破坏性操作，且仅在执行过 v1.1/v1.2 的库上才有意义。鉴于远端实际路径为 v1.0 → v2.0，**这些回滚步骤不适用，其可执行 SQL 已从本 Runbook 移除**，仅保留如下历史说明：
>
> - v1.2 回滚曾涉及删除 `uk_patient_id` / `uk_employee_id` 唯一索引与 `token_version` 列，并伴随 token_version 信息丢失的不可逆风险；
> - v1.1 回滚曾涉及备份并 `DROP` `user_account` / `patient`、`ALTER TABLE register DROP COLUMN patient_id`，存在外键逻辑断裂与账号丢失风险。
>
> 以上操作均不得在 v2.0 远端环境中执行。v2.0 失败一律遵循 §D.0：禁止删库，停止→verify→人工处理。

---

## E. 附录：生产环境变量

部署应用时需要注入以下环境变量（使用占位符，无真实值）：

| 变量 | 说明 |
|------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<REMOTE_DB_HOST>:3306/hospital_cloud_brain?...` |
| `SPRING_DATASOURCE_USERNAME` | `<REMOTE_DB_USER>` |
| `SPRING_DATASOURCE_PASSWORD` | `<REMOTE_DB_PASSWORD>` |
| `SPRING_DATA_REDIS_HOST` | `<REDIS_HOST>` |
| `SPRING_DATA_REDIS_PORT` | `<REDIS_PORT>` |
| `SPRING_DATA_REDIS_PASSWORD` | `<REDIS_PASSWORD>` |
| `HOSPITAL_JWT_SECRET` | `<JWT_SECRET_AT_LEAST_32_BYTES>` |
| `HOSPITAL_JWT_EXPIRE_HOURS` | `8` |
| `HOSPITAL_LOGIN_MAX_FAIL` | `5` |
| `HOSPITAL_LOGIN_LOCK_MINUTES` | `15` |

---

## F. 首个 ADMIN 人工开通准备（占位符模板，不含任何真实凭证）

> 本节仅提供占位符 SQL 模板。**禁止**在本文档、代码库或任何持久化介质中填入真实账号、密码、BCrypt 哈希或远端地址。真实值由负责人在执行时现场替换。

### F.1 前置条件

- v2.0 migration 已执行并通过 `v2.0_identity_schema_verify.sql`，`user_account` 表存在且初始行数 = 0。
- 已有可用的 `employee` 行供 ADMIN 绑定（或决定首个 ADMIN 暂不绑定 `employee_id`）。

### F.2 步骤 1：本地生成 BCrypt 哈希（非生产环境）

- 由负责人在**本地隔离环境**使用与应用一致的 BCrypt 实现 / cost factor，对临时初始密码生成哈希值，记作 `<BCRYPT_HASH>`。
- 生成后立即通过安全渠道将 `<BCRYPT_HASH>` 交付执行 DBA；明文密码不得写入任何文件、日志或代码库。
- 初始密码视为“一次性”，分发后即视为潜在泄露，须强制首次登录立即修改。

### F.3 步骤 2：由 DBA 在生产库执行（替换占位符后执行）

```sql
-- 占位符模板：将 <ADMIN_USERNAME> / <BCRYPT_HASH> / <EMPLOYEE_ID_OR_NULL> 替换为实际值。
-- <EMPLOYEE_ID_OR_NULL> 为已存在的 employee(id)，或字面 NULL（首个 ADMIN 暂不绑定 employee）。
INSERT INTO `user_account`
  (`username`, `password`, `role`, `employee_id`, `patient_id`,
   `status`, `token_version`, `delmark`, `create_time`, `update_time`)
VALUES
  ('<ADMIN_USERNAME>', '<BCRYPT_HASH>', 'ADMIN', <EMPLOYEE_ID_OR_NULL>, NULL,
   1, 1, 1, NOW(), NOW());
```

### F.4 步骤 3：只读校验（不返回密码哈希）

```sql
SELECT id, username, role, employee_id, status, token_version
FROM `user_account`
WHERE role = 'ADMIN';
```

### F.5 步骤 4：首次登录后强制改密

首个 ADMIN 使用初始密码登录后必须立即修改密码；改密后旧 Token 失效（`token_version` 递增），初始密码作废。
