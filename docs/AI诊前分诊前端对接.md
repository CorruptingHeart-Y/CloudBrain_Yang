# AI 诊前分诊 — 前端对接说明

> 后端模块：`com.neusoft.hospital.ai`（TriageController）
> Python 推理端点：`POST /ai/triage`（前端不直接调，由 Java 网关转发）
> 鉴权：所有接口需 `Authorization: Bearer <JWT>`（员工登录后获取）

## 一、接口清单

基地址：`/api/v1/triage`

| 方法 | 路径 | 用途 | 是否落库 |
|---|---|---|---|
| POST | `/consult` | 根据主诉推荐科室+医生 | 是 → `triage_record` |

> 分诊与处方审核不同：**每次咨询都落 `triage_record`**，没有「预览/确认」之分。AI 失败时不落记录。

## 二、调用时机

挂号前患者尚无 `register`，分诊用于辅助挂号员选择科室/医生。流程：

1. 挂号员录入患者主诉（必填）+ 患者简要信息（身份证号/病历号/姓名/年龄/性别，落记录用）。
2. 调 `POST /consult` → 后端装配全部科室 + 在岗医生作为候选集，交 AI 在候选内排序，返回带名称的推荐列表。
3. 挂号员参考推荐选择科室/医生，继续走挂号流程。

## 三、请求 / 响应

### POST /consult

请求体：

```json
{
  "chiefComplaint": "头疼三天，伴有恶心",
  "patient": { "age": 45, "gender": "男" },
  "cardNumber": "210102199001011234",
  "caseNumber": "BL20260601001",
  "patientName": "李四"
}
```

字段说明：
- `chiefComplaint`：**必填**，主诉文本。
- `patient`：可选，`{ age: int, gender: "男"|"女" }`，辅助 AI 判断。
- `cardNumber` / `caseNumber` / `patientName`：可选，**仅用于落 `triage_record`**（挂号前无 register，患者身份靠身份证号+病历号标识，无独立患者表）。不传则记录对应字段为空。

响应（`code=200` 即成功）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "departments": [
      { "deptId": 3, "deptName": "神经内科", "reason": "主诉头疼伴恶心，神经内科首诊", "score": 0.92 }
    ],
    "doctors": [
      {
        "employeeId": 12,
        "realname": "王医生",
        "deptId": 3,
        "deptName": "神经内科",
        "registLevelName": "专家号",
        "reason": "神经内科专家，擅长头痛诊疗",
        "score": 0.88
      }
    ]
  }
}
```

字段说明：
- `departments`：推荐科室列表，**按推荐度降序**，最多 5 条。
- `doctors`：推荐医生列表，**按推荐度降序**，最多 5 条。
- `score`：0~1，越高越推荐（AI 给出，仅作排序参考，不必展示给患者）。
- `reason`：AI 给出的推荐理由，可直接展示。
- `registLevelName`：医生挂号级别名（如「专家号」「普通号」），用于挂号。
- `deptName` / `realname`：由后端用候选集富化，AI 只返回 ID，前端可直接用名称展示。

### 降级（AI 不可用）

AI 服务超时/不可达时返回 503，**不落记录**：

```json
{ "code": 503, "message": "AI分诊服务暂不可用，请人工分诊", "data": null }
```

前端应提示"AI 分诊不可用，请人工分诊"，并允许挂号员手动选科室/医生继续挂号。

### 参数错误

`chiefComplaint` 为空：

```json
{ "code": 400, "message": "主诉不能为空", "data": null }
```

## 四、数据来源说明（前端无需关心，仅供理解）

- 候选科室：后端取 `department` 全表。
- 候选医生：后端取 `employee` 中 `regist_level_id` 非空的全部员工（即有挂号级别的医生）。
- AI 只能在候选集内选+排序，**不会编造不存在的科室/医生**；返回纯 ID，后端用候选集 Map 富化名称后返前端。
- 落库 `triage_record`：主诉 + 患者简要信息 + 推荐科室/医生 ID（逗号拼接）+ AI 原始返回 JSON + 操作员(employeeId，取自 JWT)。
