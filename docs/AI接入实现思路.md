# AI 接入实现思路

> 来源：基于 `AI接入的业务需求.md`（任务三/四/五）+ 现有 Spring Boot 项目结构分析。
> 生成时间：2026-06-28。

## 一、需求共性提炼（这决定了架构）

三个任务本质上**不是"对话 Agent"，而是三次结构化的 AI Service 调用**：

| 任务 | 输入 | AI 职责 | 输出 |
|---|---|---|---|
| 诊前分诊 | 主诉文本 | 在候选科室/医生中匹配并排序 | 科室列表 + 医生列表 |
| 处方审核 | 药品列表 + 患者信息 | 用药建议/相互作用/风险 | 审核结果结构化 |
| 病历生成 | 医患对话文本 | 抽取结构化字段 | 主诉/现病史/…6字段 |

共性有三点，直接影响设计：
1. **文本进、结构化 JSON 出** → 核心技术是 **function calling / JSON Schema 强制结构化输出**，不是流式聊天。流式可有可无（病历生成长一点，可选流式，其余不需要）。
2. **都是"AI 出草稿 + 人工确认"** → AI 永远不直接落库为最终结果，医生确认才写。
3. **必须用真实业务数据约束 AI**，不能让它自由发挥编科室、编医生、编药品。

所以架构比我上一轮说的更简单：Python 侧不需要 LangGraph 那种复杂编排，每个任务一个端点，`Pydantic` 定义输出 schema + 模型的结构化输出能力就够了。如果你想用框架，`pydantic-ai` 或 `LangChain` 的 structured output 都合适。

## 二、架构定位

```
前端 ──> Spring Boot (8080)                      Python AI 服务 (8000)
   /api/triage/consult      ──┐                /ai/triage
   /api/prescription/check  ──┼─内部密钥+JSON──> /ai/prescription-check
   /api/medical-record/generate──┘             /ai/medical-record
        │  鉴权/业务库读写/持久化                  纯AI推理，不碰鉴权、不写业务库
        └── 共享 MySQL(只给Spring Boot写) / Redis
```

- Spring Boot 的 `AITriageService` / `AIPrescriptionService` / `AIMedicalRecordService` 各自封装对 Python 的调用与结果解析（需求文档里点名要 `AITriageService`，就是这个角色）。
- Python 只做推理，**不写业务库**；所有持久化由 Spring Boot 完成。这样事务、鉴权、`delmark` 逻辑不分散。

## 三、各任务实现思路

### 任务三 诊前智能分诊

**关键设计**：不能让 AI 凭空报科室名。要把"当前可挂号科室 + 当日/近期排班医生"作为**候选集**一起传给 Python，让模型在候选里选+排序+给理由，返回 ID 而不是名称。

- `POST /api/triage/consult`
  - 入参：`{ chiefComplaint, patientInfo?(年龄性别) }`
  - Spring Boot 先从 `department`、`scheduling`+`employee` 查出候选科室和医生，连同主诉一起发给 Python
- Python 返回：
  ```json
  { "departments":[{"deptId":3,"reason":"..."}],
    "doctors":[{"employeeId":12,"deptId":3,"reason":"..."}] }
  ```
- 新表 `triage_record`：`id, patient_id, chief_complaint, recommend_dept_ids, recommend_doctor_ids, ai_raw_result(json), create_time`
- **需注意**：你现在的鉴权是**医院员工**登录（`AuthInterceptor` 拿的是 `employeeId`），挂号由挂号员操作。"患者 ID"从哪来需要确认——见末尾待定项。

### 任务四 AI 处方辅助审核

你现有 `prescription` 是**一行一药**（`register_id` + `drug_id` + 用法用量），一个处方 = 同一 `register_id` 的多行。

- 补传统接口：`api/prescription/create`（批量开方）、`api/prescription/list`（按挂号查）。现有 `PrescriptionController` 要看下是否已具备。
- `POST /api/prescription/check`
  - 入参：`registerId`（Spring Boot 据此聚合药品行 + 从 `register` 取患者性别/年龄 + 从 `drug_info` 取药名规格）
  - 发给 Python：`{ drugs:[{name,usage,number,format}], patient:{age,gender} }`
  - 返回：`{ suggestions:[], interactions:[{drugA,drugB,level,desc}], riskLevel, riskItems:[] }`
- 触发方式二选一（需求两种都提了）：①医生点"AI审核"预览，不落库；②保存处方时自动审核并写入审核表。建议第一版做①预览 + ②保存时落审核记录。
- 新表 `prescription_audit_record`：`id, register_id, request_snapshot(json), result_json, risk_level, auditor_employee_id, create_time`
- 降级：AI 超时/失败时返回"审核服务暂不可用，请人工核对"，不阻塞开方。

### 任务五 AI 病历自动生成

现有系统**没有病历表**，需新建。

- 新表 `medical_record`：`id, register_id, chief_complaint, present_illness, past_history, physical_exam, preliminary_diagnosis, treatment_advice, source(AI/MANUAL), create_time, update_time`
- `POST /api/medical-record/generate`：入参 `{ registerId, dialogText }` → Python 返回 6 个结构化字段 → 前端回填表单 → 医生编辑
- `POST /api/medical-record/save`、`GET /api/medical-record/list`、`GET /api/medical-record/detail`
- `preliminary_diagnosis` 可关联 `disease` 表（已存在），让 AI 从疾病字典里选。

## 四、跨任务公共设计（Python 侧）

一个 FastAPI 服务，统一：
- `X-Internal-Key` 校验中间件（和 Spring Boot 共享密钥）
- 模型客户端 + 统一超时/重试/异常降级
- 每个端点一个 `Pydantic` 输出 schema，用模型 structured output 强制结构化
- **不存任何状态**，纯函数式推理（会话历史不需要）

## 五、建议的落地顺序

1. Python 骨架 + 分诊端点跑通（最小闭环）
2. Spring Boot：`AITriageService` + `triage_record` 表 + `/api/triage/consult`
3. 处方审核（含补传统处方接口）
4. 病历生成（含 `medical_record` 表）

## 六、动工前需要拍板的点

1. **大模型 provider**（Qwen / DeepSeek / GLM / OpenAI）——决定 Python SDK 和 base_url。
2. **"患者 ID"来源**：分诊/病历都要存患者 ID，但系统现有登录是医院员工。患者是通过身份证号在前台建档、还是有单独患者表？还是先用挂号员 `employeeId` + 身份证号关联？
3. **处方审核触发方式**：第一版做"手动预览"还是"保存自动审核落库"，还是两个都做？
4. **医患对话文本来源**：病历生成的"对话文本"是医生在页面上手输，还是系统已有问诊记录模块？

这四点定了，就能开始按"五"的顺序写代码。
