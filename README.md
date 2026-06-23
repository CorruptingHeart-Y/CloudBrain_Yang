# 东软智慧云脑诊疗平台 后端服务

医院诊疗业务后端，基于 Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis 7，覆盖挂号、接诊、检查检验、处置、处方等门诊核心流程，以及科室、员工、排班、药品、疾病等基础字典管理。

> 项目实训作品 · 东软教育 · 东北大学

## 技术栈

| 层 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.2.5 / Java 17 |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL 8.0 (utf8mb4) |
| 缓存 | Redis 7 |
| API 文档 | Knife4j 4.5.0 (OpenAPI 3) |
| 构建 | Maven |
| 容器 | Docker / docker-compose (WSL) |

## 业务模块

接口统一前缀 `/api/v1`，按职责分两组：

**字典管理**

- `department` 科室
- `regist-level` 挂号级别（号别、挂号费、限额）
- `settle-category` 结算类别（医保 / 自费 / 新农合…）
- `scheduling` 排班
- `medical-technology` 医疗技术项目
- `disease` 疾病
- `drug-info` 药品信息

**业务管理**

- `employee` 医院员工（医生等）
- `register` 挂号（核心流程表，看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号）
- `check-request` 检查申请
- `inspection-request` 检验申请
- `disposal-request` 处置申请
- `prescription` 处方

数据库共 15 张核心表，建表脚本见 `sql/v1.0_init_schema.sql`。

## 工程结构

```
src/main/java/com/neusoft/hospital/
├── HospitalCloudBrainApplication.java   启动类
├── common/                              统一响应、分页、异常处理
├── config/                              Cors / Knife4j / MyBatis-Plus / Redis / 元对象填充
├── controller/                          REST 控制器（13 个业务模块）
├── service/ + service/impl/             业务层
├── mapper/                              MyBatis-Plus Mapper
├── entity/                              数据库实体
└── dto/request, dto/response            入参 / 出参对象
```

## 环境与配置

`application.yml` 只负责激活 profile 和公共配置，数据源在两个 profile 间切换：

| profile | 用途 | 数据源 |
| --- | --- | --- |
| `master` | 本地 Docker 开发 | `localhost:3306` / `localhost:6379` |
| `others` | 连接团队共享远程服务器 | `100.85.88.97` |

切换方式：修改 `application.yml` 中 `spring.profiles.active`。

MyBatis-Plus 公共行为：
- 主键 `id-type: auto`
- 逻辑删除字段 `delmark`（1-正常 / 0-已删除）
- 控制台打印 SQL

## 本地启动

### 1. 启动 MySQL / Redis（Docker）

```bash
docker compose up -d
docker compose ps          # 查看状态
docker compose logs -f mysql
```

容器首次启动会自动执行 `docker/mysql/init/01-init-schema.sql` 建库建表。

- MySQL: `localhost:3306`，root 密码 `hospital2024`，库名 `hospital_cloud_brain`
- Redis: `localhost:6379`，密码 `hospital2024`

### 2. 启动后端

```bash
./mvnw spring-boot:run
```

默认端口 `8080`。

### 3. 查看接口文档

启动后访问 Knife4j：

```
http://localhost:8080/doc.html
```

## 常用 Docker 命令

```bash
docker compose up -d            # 启动
docker compose down             # 停止
docker compose up -d --build    # 改配置后重建
docker compose down -v && docker compose up -d   # 清空数据重建
```

## 协作

- 团队共享环境时，将 `spring.profiles.active` 改为 `others`，并把 `application-others.yml` 中的 IP 改为团队服务器地址。
- 密码 `hospital2024` 仅为实训默认值，正式环境请替换。
