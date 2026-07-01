# 远端部署 Runbook（仅供人工审核，不可自动执行）

> **状态：远程数据库 `<REMOTE_DB_HOST>` 当前冻结。**  
> 本 Runbook 准备的是审核与将来执行的参考方案，文档本身 **不执行任何远端命令、不连接远端数据库**。  
> 本文中所有远端地址、账号、密码、Token、JWT secret 均使用占位符，不包含任何真实凭证。

---

## A. 部署前检查（人工）

```
1. 备份数据库
   mysqldump -h <REMOTE_DB_HOST> -u <REMOTE_DB_USER> -p<REMOTE_DB_PASSWORD> \
     --single-transaction --routines --triggers \
     hospital_cloud_brain > pre_migration_backup_YYYYMMDD.sql

2. 确认 MySQL 版本
   SELECT VERSION();  -- 期望 8.0.x

3. 确认当前仍为 v1.0（远端原基线）
   SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema='hospital_cloud_brain' AND table_name='patient';
   -- 期望 0（patient 表由 v1.1 创建）

4. 确认无未审批的手工 schema 改动
   SHOW CREATE TABLE register \G
   -- 期望无 patient_id 列

5. 在隔离环境再次验证 migration
   在独立 Docker 容器中执行 v1.0 → v1.1 → v1.1 verify → v1.2 → v1.2 verify，
   全部通过后再进入远端执行。

6. 确认首个 ADMIN 的人工创建方案
   迁移后 user_account 初始为空；首个 ADMIN 必须由 DBA 通过 SQL 手工创建。
   INSERT 语句见下文 §B 步骤 7。
```

---

## B. 推荐升级顺序

```text
1. 备份数据库
2. 执行 sql/v1.1_auth_rbac_migration.sql
   mysql -h <REMOTE_DB_HOST> -u <REMOTE_DB_USER> -p<REMOTE_DB_PASSWORD> \
     hospital_cloud_brain < sql/v1.1_auth_rbac_migration.sql
3. 执行 sql/v1.1_auth_rbac_verify.sql（只读校验）
   必须通过：
     - patient / user_account 表存在
     - register.patient_id 列与索引存在
     - user_account 行数 = 0
     - 回填/冲突/异常统计符合预期
4. 人工处理 patient_id 未回填异常记录
   - 12 位异常 cardNumber → 确认为历史数据或补正
   - 冲突组 → 人工选择归并或保留 NULL
5. 执行 sql/v1.2_account_lifecycle_migration.sql
   mysql -h <REMOTE_DB_HOST> -u <REMOTE_DB_USER> -p<REMOTE_DB_PASSWORD> \
     hospital_cloud_brain < sql/v1.2_account_lifecycle_migration.sql
   若因重复绑定中止：
     - token_version 列已加，uk_employee_id/uk_patient_id 未建
     - 无遗留存储过程 routine
     - 运行 v1.2_account_lifecycle_verify.sql 查看重复明细
     - 人工去重后重跑（幂等）
6. 执行 sql/v1.2_account_lifecycle_verify.sql（只读校验）
   必须通过：
     - token_version 列存在
     - uk_employee_id / uk_patient_id 存在
     - 重复绑定计数 = 0
7. 人工创建首个 ADMIN 账号
   -- 密码使用 BCrypt 哈希，不可明文
   INSERT INTO user_account (username, password, role, employee_id, patient_id,
     status, token_version, delmark, create_time, update_time)
   VALUES ('admin', '<BCRYPT_HASH>', 'ADMIN', NULL, NULL,
     1, 1, 1, NOW(), NOW());
   -- 首个 ADMIN 密码经安全渠道分发，强制首次登录立即修改
8. 部署应用代码
   - 编译：mvn -DskipTests clean package
   - 替换 JAR / Docker 镜像
   - 注入生产环境变量（JWT secret、DB 密码、Redis 密码）
9. 三角色登录与权限冒烟（见 §C）
```

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

### D.1 应用代码回滚

```
1. 恢复上一版 JAR / Docker 镜像
2. 但注意：若 v1.2 migration 已执行，token_version 列已存在，
   旧应用代码的 UserAccount 实体缺少该字段 → MyBatis-Plus 映射可能失败。
   若旧代码使用 selectById/selectList 自动映射所有列，token_version 为 null 可能无碍；
   但若旧代码 mapper 有显式字段列表，需确认兼容性。
   建议：应用代码回滚前先在隔离环境验证旧代码在 v1.2 schema 上能否启动。
```

### D.2 v1.2 schema 回滚

```sql
-- 仅当需要完全回退 v1.2 migration 时执行（人工审核后方可执行）

-- 1. 删除 uk_patient_id 唯一索引
ALTER TABLE `user_account` DROP INDEX `uk_patient_id`;

-- 2. 删除 uk_employee_id 唯一索引
ALTER TABLE `user_account` DROP INDEX `uk_employee_id`;

-- 3. 删除 token_version 列
ALTER TABLE `user_account` DROP COLUMN `token_version`;

-- ⚠️ 上述回滚不可逆：若回滚后又重新执行 v1.2，
--    token_version 重新从默认值 1 开始，旧账号的 token_version 信息丢失。
--    回滚后所有用户的 JWT tv claim 与 token_version 比对将失效（取决于应用代码版本）。
```

### D.3 v1.1 schema 回滚（谨慎）

```
v1.1 回滚涉及已回填的 register.patient_id、已创建的 patient 表和 user_account 表。

警告：
- 不能直接 DROP patient 表 —— register.patient_id 指向 patient.id，
  直接 DROP 后外键逻辑无法重建
- 如果 DOCTOR/ADMIN/PATIENT 账号已投入使用，DROP user_account 会丢失所有账号，
  必须执行全量 user_account 备份导出

v1.1 回滚步骤（需 DBA 确认所有关联应用已下线）：
1. 备份 register 的 patient_id 回填值（若有）
   SELECT id, patient_id FROM register WHERE patient_id IS NOT NULL
     INTO OUTFILE '/tmp/register_patient_backup.csv';
2. 备份 user_account 全量
   mysqldump ... hospital_cloud_brain user_account > user_account_backup.sql
3. 备份 patient 表
4. DROP TABLE IF EXISTS user_account;
5. DROP TABLE IF EXISTS patient;
6. ALTER TABLE register DROP COLUMN patient_id, DROP INDEX idx_patient_id;

必须停下来人工处理的情况：
- register 中已有 patient_id 指向 patient，直接 DROP patient 会导致逻辑断裂
- 已有 PROD 账号使用中，DROP user_account 前必须确认有离线验证的备份恢复方案
```

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
