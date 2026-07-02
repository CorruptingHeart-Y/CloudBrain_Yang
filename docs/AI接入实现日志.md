# AI 接入实现日志

> 伴随实现过程逐步记录。架构与思路详见 `AI接入实现思路.md`，本文件只记"做了什么"。

## 决策定稿（2026-06-29）

经核对 `sql/v1.0_init_schema.sql` 与现有 Java 代码后，四个待决策点全部敲定：

| 决策点 | 结论 |
|---|---|
| 1. 大模型 provider | **GLM**（智谱）。Python 侧用官方 SDK `zhipuai`，`base_url=https://open.bigmodel.cn/api/paas/v4`，模型 `glm-4-plus`，JSON 模式结构化输出。API Key 仅放 Python 侧环境变量。 |
| 2. "患者 ID"来源 | **无独立患者表**，患者身份 = `register.card_number`(身份证号) + `case_number`(病历号)。病历复用 `medical_record.register_id` 外键；诊前分诊（挂号前无 register）直接落 `card_number`+姓名/性别/年龄，操作员记 `employee_id`。 |
| 3. 处方审核触发方式 | 第一版 **①手动预览 + ②保存时落 `prescription_audit_record`** 两者都做；AI 失败降级"请人工核对"，不阻塞开方。 |
| 4. 医患对话文本来源 | **医生页面手输/粘贴**（系统无问诊记录表）。 |

附带的工程决策：

- **Python 依赖管理**：`pyproject.toml` + **uv**。
- **Python 代码位置**：仓库根同级目录 `ai-service/`，并接入 `docker-compose.yml`，端口 8000，纯推理不碰 MySQL/Redis。
- **Java 包结构**：仿 `auth` 模块建 `com.neusoft.hospital.ai` 特性模块（config/client/controller/service/service.impl/dto），实体与 Mapper 仍放全局 `entity/`、`mapper/` 包。
- **API 路径**：跟随现有 `/api/v1/...` 前缀。Python 侧路由 `/ai/triage`、`/ai/prescription-check`、`/ai/medical-record`。
- **`medical_record` 加列**：`ALTER` 增加 `source CHAR(1)`（A=AI草稿 / M=人工），随任务五 Java 步骤一并落库。

### 现有代码核查结论（与上一轮文档出入）

1. `medical_record` 表**已存在**（schema 第 11 张表，9 个临床字段 + `medical_record_disease` 关联 `disease`），Java 侧尚无 entity/mapper。→ 任务五**不建表**，只补 Java 层，AI 输出映射到现有字段。
2. `prescription` 已有完整 CRUD（`PrescriptionController` `/api/v1/prescription`，含新增 + 按挂号查列表）。→ 任务四**不补处方接口**，只加 AI 审核端点 + `prescription_audit_record` 表。

### 拟新增表（DDL 草案，随对应 Java 步骤落库）

- `triage_record`（任务二）
- `prescription_audit_record`（任务三）
- `medical_record` 仅 ALTER 加 `source` 列（任务四）

---

## 实现进度

### Step ① Python 骨架 + GLM 分诊端点（2026-06-29）

**目标**：搭好 `ai-service/` 工程，跑通"Spring Boot → Python `/ai/triage` → GLM 结构化输出"最小闭环（GLM 真实调用需配 Key）。

**新增文件**：
- `ai-service/pyproject.toml` — uv 工程定义，依赖 fastapi/uvicorn/pydantic/pydantic-settings/zhipuai/httpx。
- `ai-service/.python-version` — 锁 3.12。
- `ai-service/.env.example` — GLM_API_KEY / GLM_BASE_URL / GLM_MODEL / INTERNAL_KEY / 超时/重试。
- `ai-service/app/config.py` — `pydantic-settings` 读环境变量。
- `ai-service/app/main.py` — FastAPI 入口 + `X-Internal-Key` 中间件 + `/health`。
- `ai-service/app/clients/glm_client.py` — GLM 封装：JSON 模式结构化输出 + 超时 + 重试 + 解析失败/异常降级为 `AiInferenceError`。
- `ai-service/app/schemas/triage.py` — 分诊请求/响应 Pydantic 模型（camelCase 别名，对齐 Jackson）。
- `ai-service/app/services/triage_service.py` — 拼 prompt（候选集约束）→ 调 GLM。
- `ai-service/app/routers/triage.py` — `POST /ai/triage`，失败返回 503 友好提示。
- `ai-service/Dockerfile`、`ai-service/README.md`。
- `docker-compose.yml` — 新增 `ai-service` 服务（端口 8000，与后端同网络）。

**关键设计**：
- Python 不存状态，纯函数式推理；候选科室/医生由 Spring Boot 传入，GLM 只能在候选集内选+排序+给理由，返回 ID。
- 未配 `GLM_API_KEY` 时 `GlmClient.available=False`，调用直接抛 `AiInferenceError` → 路由 503，不致崩。

**运行方式**：
```bash
cd ai-service
cp .env.example .env   # 填入 GLM_API_KEY 与和后端一致的 INTERNAL_KEY
uv sync
uv run uvicorn app.main:app --reload --port 8000
```

**状态**：骨架与分诊端点完成并本地校验通过。
- `uv sync` 成功安装依赖（fastapi 0.138 / pydantic 2.13 / pydantic-settings 2.14 / zhipuai 2.1.5 / uvicorn 0.49 等）。
- TestClient 验证：`GET /health` → 200（`glm_configured=false`）；`POST /ai/triage` 无 `X-Internal-Key` → 401；带密钥但未配 GLM Key → 503 降级提示；全部模块导入正常。
- 修复：`app/main.py` 漏导 `get_glm_client`，已补。
- 待用户：填 `ai-service/.env` 的 `GLM_API_KEY` 与和后端一致的 `INTERNAL_KEY` 后即可跑真实 GLM 调用闭环。

### Step ② Spring Boot 诊前分诊（2026-06-30）

**目标**：补齐 Java 侧分诊闭环——`POST /api/v1/triage/consult`：装配候选集 → 调 Python `/ai/triage` → 富化结果 → 落 `triage_record`；AI 失败降级。

**新增文件**：
- `sql/v1.1_triage_record.sql` — 诊前分诊记录表 DDL（版本化增量脚本，沿用 v1.0 风格；未硬切库，注释提示在应用库 `hospital_cloud_brain` 下执行）。
- `entity/TriageRecord.java` — 全局实体（`creationTime` 手动赋值，不依赖 `MetaObjectHandler`）。
- `mapper/TriageRecordMapper.java` — `@Mapper` + `BaseMapper`。
- `ai/config/AiProperties.java` — `@ConfigurationProperties("hospital.ai")`：baseUrl/internalKey/triagePath/timeoutSeconds。
- `ai/config/RestClientConfig.java` — `RestClient` Bean，默认带 `X-Internal-Key` 头 + 超时。
- `ai/client/TriageClient.java` — 调 Python，异常映射：401→密钥配置错误(系统错误)、503/超时/不可达→`AI_UNAVAILABLE`。
- `ai/dto/` — `PatientBrief`、`TriageConsultRequest`(前端入参)、`AiTriageRequest`/`AiTriageResponse`(对齐 Python camelCase 契约)、`TriageResultDTO`(富化后返前端，含科室名/医生名/挂号级别名)。
- `ai/service/TriageService` + `impl/TriageServiceImpl` — 装配候选集(`DepartmentService.listAll` + `EmployeeService.list(isNotNull(registLevelId))` + `RegistLevelService.listAll` 建 ID→名称 Map)→ 调 client → 富化 → 落记录。
- `ai/controller/TriageController.java` — `POST /api/v1/triage/consult`，`@Tag("AI诊前分诊")` + `@SecurityRequirement`。

**改动文件**：
- `common/ErrorCode.java` — 增 `AI_UNAVAILABLE(503, "AI分诊服务暂不可用，请人工分诊")`。
- `application.yml` — `hospital.ai` 配置段（`internal-key` 默认占位值与 `ai-service/.env` 对齐，可用 `HOSPITAL_AI_INTERNAL_KEY` 环境变量覆盖）。
- `config/Knife4jConfig.java` — 新增"AI诊前分诊"分组。

**关键设计**：
- 候选医生 = `regist_level_id` 非空的全部员工（scheduling 表仅周规则字符串、无日期映射，无法判"今日出诊"；先取全集，后续可加过滤）。
- AI 返回纯 ID，Java 侧用候选集 Map 富化名称后返前端；`triage_record` 落 `recommend_dept_ids`/`recommend_doctor_ids`(逗号拼接) + `ai_raw_result`(完整 JSON) + 操作员 `CurrentUser.require()`。
- 降级：Python 503/超时/不可达 → 抛 `BusinessException(AI_UNAVAILABLE)`，`GlobalExceptionHandler` 转 `Result.fail(503, "AI分诊服务暂不可用，请人工分诊")`，**不落记录**；401(密钥不一致)→系统错误提示，便于开发期排查。
- 患者身份沿用决策2：分诊落 `card_number`+`case_number`+姓名/性别/年龄，操作员记 `employee_id`。

**状态**：`mvn clean compile` 通过（150 源文件）。待运行时验证：
1. 在 `hospital_cloud_brain` 库执行分诊表 DDL（手动执行 `sql/v1.1_triage_record.sql`，或全新建库时由 `docker/mysql/init/02-triage-record.sql` 自动执行）。
2. 本地启动 Python（`uv run uvicorn app.main:app --reload --port 8000`）。
3. 降级路径：未配 `GLM_API_KEY` → `/api/v1/triage/consult` 返回 503 友好提示。
4. 正常路径：填真实 `GLM_API_KEY` + 一致 `INTERNAL_KEY` → 返回带名称的推荐列表，`triage_record` 落库。

**附带修复（docker-compose 挂载错位）**：原 `docker-compose.yml` 的 MySQL 挂载指向空的根目录 `./mysql/...`，未指向真实配置 `./docker/mysql/...`，导致 `my.cnf` 未加载、建表脚本未自动执行。已将 MySQL 两个挂载改指 `./docker/mysql/init` 与 `./docker/mysql/my.cnf`（与 redis 的 `./docker/redis/redis.conf` 写法对齐），删除空的根目录 `mysql/`、`redis/`，并把分诊表 DDL 同步放入 `docker/mysql/init/02-triage-record.sql`（首次建库自动执行）。注：init 脚本仅首次建库执行，现有库仍需手动跑一次 DDL。

**待办**：Step③ 处方审核 + `prescription_audit_record`；Step④ 病历生成（Java 层 + `medical_record` ALTER 加 `source`）。

### Step ③ AI 处方辅助审核（2026-07-01）

**目标**：补齐处方审核闭环——`POST /api/v1/prescription/check`：按挂号聚合处方明细 + 患者信息 + 药名规格 → 调 Python `/ai/prescription-check` → 富化药名 → 预览或落 `prescription_audit_record`；AI 失败降级不阻塞开方。

**Python 侧新增文件**：
- `ai-service/app/schemas/prescription.py` — 请求/响应 Pydantic 模型（camelCase alias）：`PrescriptionCheckRequest`(registerId/patient/drugs[])、`PrescriptionCheckResult`(riskLevel/suggestions/interactions/riskItems)。
- `ai-service/app/services/prescription_service.py` — SYSTEM_PROMPT 约束 GLM 仅基于传入药品审核、riskLevel 取值 low/medium/high、空数组返回 `[]`，调 GLM json_object 结构化输出。
- `ai-service/app/routers/prescription.py` — `POST /ai/prescription-check`，AiInferenceError → 503。
- `ai-service/app/main.py` — 注册 prescription 路由（prefix `/ai`）。

**Java 侧新增文件**：
- `sql/v1.2_prescription_audit_record.sql` — 处方审核记录表 DDL（增量脚本，沿用 v1.1 风格；不硬切库）。
- `docker/mysql/init/03-prescription-audit-record.sql` — Docker 首次建库自动执行副本。
- `entity/PrescriptionAuditRecord.java` + `mapper/PrescriptionAuditRecordMapper.java` — 全局实体/Mapper。
- `ai/dto/AiPrescriptionCheckRequest` / `AiPrescriptionCheckResponse` — 对齐 Python camelCase 契约（响应仅 drugId，药名 Java 富化）。
- `ai/dto/PrescriptionCheckRequest` — 前端入参（registerId）。
- `ai/dto/PrescriptionAuditResultDTO` — 富化后返前端（含 drugName）。
- `ai/client/PrescriptionAuditClient.java` — 调 Python，异常映射同 TriageClient：401→密钥配置错误、503/超时→`AI_AUDIT_UNAVAILABLE`。
- `ai/service/PrescriptionAuditService` + `impl/PrescriptionAuditServiceImpl` — `audit(registerId, persist)`：①聚合 prescription 明细行 ②drug_info 取药名/规格建 Map ③register 取患者性别/年龄 ④调 Python ⑤富化 ⑥按 persist 落库。
- `ai/controller/PrescriptionAuditController.java` — `POST /api/v1/prescription/check`(预览不落库) + `POST /api/v1/prescription/check/confirm`(审核并落库) + `GET /api/v1/prescription/check/record?registerId=`(查审核留痕，按时间倒序)。
- `ai/dto/PrescriptionAuditRecordDTO.java` — 审核记录响应 DTO（requestSnapshot/resultJson 为 JSON 字符串，前端按需解析）。

**改动文件**：
- `common/ErrorCode.java` — 增 `AI_AUDIT_UNAVAILABLE(503, "AI处方审核服务暂不可用，请人工核对")`。
- `ai/config/AiProperties.java` — 增 `prescriptionCheckPath = "/ai/prescription-check"`。
- `application.yml` — `hospital.ai` 增 `prescription-check-path`。
- `config/Knife4jConfig.java` — `aiApi` 分组改名"AI智能辅助"，纳入 `/api/v1/prescription/check/**`。

**关键设计**：
- 触发方式（决策3）：①手动预览(`/check`，不落库)；②确认/保存处方时(`/check/confirm`，落 `prescription_audit_record`)。两者共用 `audit(registerId, persist)`。
- AI 返回纯 drugId，Java 侧用 drug_info 候选集 Map 富化药名后返前端；`prescription_audit_record` 落 `request_snapshot`(发往Python的请求JSON) + `result_json`(AI完整返回) + `risk_level` + 操作员 `CurrentUser.require()`。
- 降级：Python 503/超时/不可达 → 抛 `BusinessException(AI_AUDIT_UNAVAILABLE)`，转 `Result.fail(503, "AI处方审核服务暂不可用，请人工核对")`，**不落记录**；401(密钥不一致)→系统错误提示。
- 该挂号下无处方明细 → `PARAM_ERROR` "该挂号下无处方明细，无法审核"。

**状态**：`mvn clean compile` 通过；运行时闭环已验证(2026-07-01)：
- Python `/health` `glm_configured:true`；直连 `/ai/prescription-check` 返回结构化结果(riskLevel/suggestions/interactions/riskItems)。
- Java `POST /api/v1/prescription/check`(registerId=14, 两药) 预览返回 riskLevel + 富化药名；`POST /check/confirm` 落库(用户已校验 `prescription_audit_record` 有记录)。
- `GET /check/record?registerId=` 查询留痕已加(重建 Java 后生效)。
- 前端对接契约见 `docs/AI处方审核前端对接.md`（注意 `docs/` 在 .gitignore，为本地工作文档，需另行分享给前端）。

**待办**：Step④ 病历生成（Java 层 + `medical_record` ALTER 加 `source`）。

### Step ④ AI 病历生成（2026-07-01）

**目标**：补齐病历生成闭环——`POST /api/v1/medical-record/generate`：医患对话文本（医生手输/粘贴，决策4）+ 患者信息 → 调 Python `/ai/medical-record` → 9 字段病历草稿（仅预览）；`POST /api/v1/medical-record`：医生确认后 upsert `medical_record` + 替换 `medical_record_disease` 疾病关联；AI 失败降级请人工书写。

**Python 侧新增文件**：
- `ai-service/app/schemas/medical_record.py` — 请求/响应 Pydantic 模型（camelCase alias）：`MedicalRecordRequest`(registerId/patient/dialogue)、`MedicalRecordDraft`(9 临床字段)。
- `ai-service/app/services/medical_record_service.py` — SYSTEM_PROMPT 约束 GLM 仅基于对话归纳、未提及字段返回空串、输出 10 键 JSON，调 GLM json_object 结构化输出。
- `ai-service/app/routers/medical_record.py` — `POST /ai/medical-record`，AiInferenceError → 503。
- `ai-service/app/main.py` — 注册 medical_record 路由（prefix `/ai`）。

**Java 侧新增文件**：
- `sql/v1.3_medical_record_source.sql` — `medical_record` ALTER 加 `source CHAR(1) DEFAULT 'M'`（A=AI草稿/M=人工），增量脚本风格同 v1.1/v1.2。
- `docker/mysql/init/04-medical-record-source.sql` — Docker 首次建库自动执行副本。
- `entity/MedicalRecord.java` + `mapper/MedicalRecordMapper.java` — 病历实体/Mapper（含 source；表无时间列，故无 creationTime）。
- `entity/MedicalRecordDisease.java` + `mapper/MedicalRecordDiseaseMapper.java` — 病历-疾病关联实体/Mapper。
- `ai/dto/AiMedicalRecordRequest` / `AiMedicalRecordResponse` — 对齐 Python camelCase 契约。
- `ai/dto/MedicalRecordGenerateRequest` — 前端入参（registerId 必填 + dialogue 必填）。
- `ai/dto/MedicalRecordDraftDTO` — AI 草稿响应（9 字段，纯文本无需富化）。
- `ai/dto/MedicalRecordSaveRequest` — 保存入参（9 字段 + source + diseaseIds）。
- `ai/dto/MedicalRecordDTO` — 落库/查询响应（含 diseases 列表：id/name/ICD）。
- `ai/client/MedicalRecordClient.java` — 调 Python，异常映射同 TriageClient/PrescriptionAuditClient：401→密钥配置错误、503/超时→`AI_MEDICAL_UNAVAILABLE`。
- `ai/service/MedicalRecordService` + `impl/MedicalRecordServiceImpl` — `generate`(AI 出草稿不落库)、`save`(@Transactional：upsert by registerId + 替换疾病关联)、`getByRegisterId`(含疾病富化)。
- `ai/controller/MedicalRecordController.java` — `POST /api/v1/medical-record/generate`(预览) + `POST /api/v1/medical-record`(保存) + `GET /api/v1/medical-record?registerId=`(查询)。

**改动文件**：
- `common/ErrorCode.java` — 增 `AI_MEDICAL_UNAVAILABLE(503, "AI病历生成服务暂不可用，请人工书写")`。
- `ai/config/AiProperties.java` — 增 `medicalRecordPath = "/ai/medical-record"`。
- `application.yml` — `hospital.ai` 增 `medical-record-path`。
- `config/Knife4jConfig.java` — `aiApi` 分组纳入 `/api/v1/medical-record/**`。

**关键设计**：
- `medical_record` 表已存在（v1.0 第 11 张表，9 临床字段 + `uk_register_id` 唯一约束 = 一挂号一病历），故**不建表**，只 ALTER 加 `source` 列区分来源。
- 流程「AI 出草稿 + 人工确认」：`/generate` 仅预览不落库；医生编辑草稿 + 从疾病字典选 ICD 疾病后调 `/` 保存。保存接口**不再调 AI**（避免医生已审阅的草稿被二次推理改变），是纯持久化。
- AI 不挑疾病 ID：疾病表大、ICD 编码属人工责任，AI 仅出 `diagnosis` 文本辅助医生在 UI 选病；疾病关联由前端把 `diseaseIds` 一并提交，后端先删旧再插新（去重）。
- 保存按 `register_id` upsert：存在则 `updateById`，否则 `insert`；`source` 缺省按 `M`（人工）。
- 降级：Python 503/超时/不可达 → 抛 `BusinessException(AI_MEDICAL_UNAVAILABLE)`，转 `Result.fail(503, "AI病历生成服务暂不可用，请人工书写")`，**不落库**；保存接口不依赖 AI，不受影响。

**状态**：`mvn clean compile` 通过（175 源文件）；运行时闭环待验证：
1. 在 `hospital_cloud_brain` 库执行 `sql/v1.3_medical_record_source.sql`（或全新建库时由 `docker/mysql/init/04-medical-record-source.sql` 自动执行）。
2. 本地启动 Python（`./dev.sh` 或 `uv run uvicorn app.main:app --port 8000`）。
3. 降级路径：未配 `GLM_API_KEY` → `/api/v1/medical-record/generate` 返回 503 友好提示。
4. 正常路径：填真实 `GLM_API_KEY` + 一致 `INTERNAL_KEY` → `/generate` 返回 9 字段草稿；`POST /api/v1/medical-record` 落库 + 疾病关联；`GET /?registerId=` 回读校验。
- 前端对接契约见 `docs/AI病历生成前端对接.md`（注意 `docs/` 在 .gitignore，为本地工作文档，需另行分享给前端）。

**四个 AI 结构化任务全部完成**（分诊 / 处方审核 / 病历生成），`ai-integration-plan` Step①②③④ 落地。
