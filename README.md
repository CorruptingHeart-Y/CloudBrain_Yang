# 东软智慧云脑诊疗平台 后端服务

医院诊疗业务后端，基于 Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis 7，覆盖挂号、接诊、检查检验、处置、处方等门诊核心流程，以及科室、员工、排班、药品、疾病等基础字典管理。已完成 JWT v2 账号化认证（ADMIN / DOCTOR / PATIENT 三角色）、医生数据范围隔离、患者门户、账号生命周期与患者自助注册。

> 项目实训作品 · 东软教育 · 东北大学

## 技术栈

| 层 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.2.5 / Java 17 |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL 8.0 (utf8mb4 / utf8mb4_0900_ai_ci) |
| 缓存 | Redis 7 |
| 认证 | JWT v2（ver=2 + role + tv + accountId） |
| API 文档 | Knife4j 4.5.0 (OpenAPI 3) |
| 构建 | Maven |
| 容器 | Docker / docker-compose |

## 业务模块

接口统一前缀 `/api/v1`，按角色与职责分组：

### 认证（@Public + 三角色）

| 接口 | 角色 |
|------|------|
| `POST /auth/login` | 公开 |
| `POST /auth/logout` | ADMIN / DOCTOR / PATIENT |
| `GET /auth/me` | 三角色 |
| `PUT /auth/password` | 三角色（改后旧 Token 立即 401） |
| `POST /auth/patient/register` | 公开（患者自助注册） |

### 账号管理（仅 ADMIN）

| 接口 | 说明 |
|------|------|
| `/api/v1/account/**` | 创建/查询/启禁用/重置密码；不返密码/身份证/手机号/地址 |

### 字典管理

| 模块 | 角色 | 说明 |
|------|------|------|
| `department` | 仅 ADMIN | 科室 |
| `regist-level` | 仅 ADMIN | 挂号级别（号别/挂号费/限额） |
| `settle-category` | 仅 ADMIN | 结算类别 |
| `scheduling` | 仅 ADMIN | 排班 |
| `employee` | 仅 ADMIN | 员工（医生） |
| `drug-info` | ADMIN + DOCTOR | 药品信息 |
| `disease` | ADMIN + DOCTOR | 疾病（含 ICD 编码） |
| `medical-technology` | ADMIN + DOCTOR | 医技项目 |

### 诊疗业务（ADMIN + DOCTOR；DOCTOR 仅本人接诊范围）

| 模块 | 说明 |
|------|------|
| `register` | 挂号（看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号） |
| `check-request` | 检查申请 |
| `inspection-request` | 检验申请 |
| `disposal-request` | 处置申请 |
| `prescription` | 处方 |

> DOCTOR 访问非本人接诊的 register 或派生记录 → HTTP 404（不透露是否存在）。

### 患者门户（仅 PATIENT）

| 接口 | 说明 |
|------|------|
| `GET /patient/profile` | 个人资料（无身份证/手机号/地址） |
| `GET /patient/records` | 就诊记录列表（仅 `register.patient_id = 自己`） |
| `GET /patient/records/{registerId}` | 就诊详情（病历/处方/检查/检验/处置） |

## 数据库

- 基础表 v1.0：15 张（`sql/v1.0_init_schema.sql`）
- PR1 v1.1 migration：`patient` / `user_account` 表 + `register.patient_id` 列与历史归并（`sql/v1.1_auth_rbac_migration.sql`）
- PR5 v1.2 migration：`token_version` 列 + `uk_employee_id` / `uk_patient_id` 唯一索引（`sql/v1.2_account_lifecycle_migration.sql`）
- Docker 默认启动只用 v1.0 init（远端同款 15 表基线），v1.1/v1.2 为人工执行的候选迁移

## 工程结构

```
src/main/java/com/neusoft/hospital/
├── HospitalCloudBrainApplication.java   启动类
├── auth/                                JWT v2 认证内核
│   ├── annotation/                      @Public / @RequireRole
│   ├── context/                         AuthUser / CurrentUser (ThreadLocal)
│   ├── controller/                      AuthController（登录/登出/改密/患者注册）
│   ├── dto/                             登录/注册/改密 DTO
│   ├── enums/                           Role（ADMIN/DOCTOR/PATIENT 平级）
│   ├── interceptor/                     AuthInterceptor（Token 校验 + 角色授权）
│   ├── jwt/                             JwtUtil（签发/解析/ver=2+role+tv）
│   └── service/                         AuthService（登录/登出/改密/注册）
├── common/                              统一响应 Result / 分页 / 异常处理 / ErrorCode
├── config/                              Cors / Knife4j / MyBatis-Plus / Redis / 元对象填充
├── controller/                          15 个业务 Controller（含 PatientController、AccountController）
├── service/ + service/impl/             业务层（含 RegisterOwnership 医生归属组件、PatientPortalService）
├── mapper/                              MyBatis-Plus Mapper（含 UserAccountMapper、MedicalRecordMapper）
├── entity/                              数据库实体（含 UserAccount、MedicalRecord）
└── dto/request, dto/response            入参 / 出参对象
```

## 环境与配置

`application.yml` 只负责激活 profile 和公共配置，数据源在 profile 间切换：

| profile | 用途 | 数据源 |
| --- | --- | --- |
| `master` | 本地 Docker 开发 | `127.0.0.1:3306` / `127.0.0.1:6379` |
| `others` | 连接团队共享远程服务器 | `<REMOTE_HOST>` |
| `test` | 集成测试（本地隔离库） | `127.0.0.1:3306/hospital_rbac_v11` |

> 远程数据库当前冻结；生产部署顺序见 `docs/deployment-runbook.md`。

MyBatis-Plus 公共行为：主键 `id-type: auto`、逻辑删除字段 `delmark`（1-正常 / 0-已删除）。

## 本地启动

### 1. 启动 MySQL / Redis（Docker）

```bash
docker compose up -d
```

容器首次启动自动执行 `docker/mysql/init/01-init-schema.sql` 建库（15 张 v1.0 表）。

- MySQL: `localhost:3306`，root 密码 `hospital2024`，库名 `hospital_cloud_brain`
- Redis: `localhost:6379`，密码 `hospital2024`

### 2. 运行测试

```bash
mvn test     # 62 个集成测试（需要本地 MySQL + Redis）
```

### 3. 查看接口文档

```bash
mvn spring-boot:run
# 浏览器打开 http://localhost:8080/doc.html
```

## 文档

| 文档 | 说明 |
|------|------|
| `docs/前端登录鉴权对接.md` | 前端对接契约（登录/Token/401/403/角色路由/患者注册/账号管理） |
| `docs/deployment-runbook.md` | 远端部署 Runbook（迁移顺序/冒烟/回滚，仅供人工审核） |

## 常用命令

```bash
docker compose up -d                     # 启动
docker compose down                      # 停止
docker compose down -v && docker compose up -d  # 清空数据重建
mvn test                                 # 运行测试
mvn -DskipTests compile                  # 仅编译
```
