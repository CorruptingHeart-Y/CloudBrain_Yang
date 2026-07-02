# AI 病历生成 — 前端对接说明

> 后端模块：`com.neusoft.hospital.ai`（MedicalRecordController）
> Python 推理端点：`POST /ai/medical-record`（前端不直接调，由 Java 网关转发）
> 鉴权：所有接口需 `Authorization: Bearer <JWT>`（员工登录后获取）

## 一、接口清单

基地址：`/api/v1/medical-record`

| 方法 | 路径 | 用途 | 是否落库 |
|---|---|---|---|
| POST | `/generate` | 医患对话文本 → AI 生成病历草稿 | 否 |
| POST | `/` | 保存/确认病历（含疾病关联） | 是 → `medical_record` + `medical_record_disease` |
| GET  | `/?registerId=` | 查询某挂号已落库病历 | — |

## 二、调用时机（重要）

本系统无问诊记录表，**医患对话文本由医生在页面手输/粘贴**（决策4）。流程遵循「AI 出草稿 + 人工确认」：

1. 医生看完诊，在病历页文本框粘贴/录入医患对话文本。
2. 点「AI 生成草稿」→ 调 `POST /generate`，后端装配患者性别/年龄（取自挂号）+ 对话文本交 AI，返回 9 字段病历草稿。**仅预览，不落库。**
3. 医生在表单里编辑草稿各字段，并从疾病字典选择 ICD 疾病（可多选）。
4. 点「保存病历」→ 调 `POST /`，后端按挂号 upsert `medical_record` + 替换 `medical_record_disease` 关联，`source=A`。
5. 若医生不走 AI、直接手写：跳过步骤 2-3，直接 `POST /` 提交手写内容，`source=M`。

> `medical_record` 表对 `register_id` 有唯一约束：**一个挂号一份病历**。重复保存即更新同一条。
> AI 不挑疾病 ID（疾病表大、ICD 编码是人工责任）；AI 的 `diagnosis` 仅出文本，辅助医生在 UI 里选病。疾病关联由前端把医生选的 `diseaseIds` 一并提交。

## 三、请求 / 响应

### POST /generate

请求体：

```json
{
  "registerId": 14,
  "dialogue": "患者男，45岁，诉头疼三天，伴恶心……（医患对话全文）"
}
```

字段说明：
- `registerId`：**必填**，挂号ID。后端据此取患者性别/年龄辅助 AI。
- `dialogue`：**必填**，医患对话文本。

响应（`code=200` 即成功）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "readme": "头疼三天，伴恶心",
    "present": "患者三天前无明显诱因出现头痛……",
    "presentTreat": "来诊前未用药",
    "history": "高血压病史5年",
    "allergy": "无",
    "physique": "血压150/95mmHg，神志清",
    "proposal": "建议头颅CT、血常规",
    "careful": "注意休息，低盐饮食，一周后复诊",
    "diagnosis": "偏头痛；高血压病",
    "cure": "对症止痛治疗，继续降压"
  }
}
```

字段说明：9 个临床字段，对齐 `medical_record` 表。对话未提及的字段 AI 返回空字符串 `""`。前端把这些填入病历表单供医生编辑。

### POST /（保存/确认）

请求体：

```json
{
  "registerId": 14,
  "readme": "头疼三天，伴恶心",
  "present": "患者三天前无明显诱因出现头痛……",
  "presentTreat": "来诊前未用药",
  "history": "高血压病史5年",
  "allergy": "无",
  "physique": "血压150/95mmHg，神志清",
  "proposal": "建议头颅CT、血常规",
  "careful": "注意休息，低盐饮食，一周后复诊",
  "diagnosis": "偏头痛；高血压病",
  "cure": "对症止痛治疗，继续降压",
  "source": "A",
  "diseaseIds": [3, 7]
}
```

字段说明：
- `registerId`：**必填**。
- 9 个临床字段：医生编辑后的最终内容（可空）。
- `source`：`A`=AI草稿来源，`M`=人工录入。走 AI 流程传 `A`，纯手写传 `M`；不传按 `M`。
- `diseaseIds`：医生从疾病字典选的疾病ID列表，可空/不传。后端会**先删旧关联再插新**（去重）。

响应：返回落库后的完整病历（结构同 `GET /`）。

### GET /?registerId=14

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "registerId": 14,
    "readme": "头疼三天，伴恶心",
    "present": "...",
    "presentTreat": "...",
    "history": "...",
    "allergy": "...",
    "physique": "...",
    "proposal": "...",
    "careful": "...",
    "diagnosis": "...",
    "cure": "...",
    "source": "A",
    "diseases": [
      { "id": 3, "diseaseName": "偏头痛", "diseaseICD": "G43.9" },
      { "id": 7, "diseaseName": "高血压病", "diseaseICD": "I10" }
    ]
  }
}
```

- 该挂号未落库时 `data` 为 `null`（`code` 仍 200）。
- `diseases` 按 diseaseId 升序。

### 降级（AI 不可用）

`POST /generate` 在 AI 服务超时/不可达时返回 503，**不落库**：

```json
{ "code": 503, "message": "AI病历生成服务暂不可用，请人工书写", "data": null }
```

前端应提示"AI 病历生成不可用，请人工书写"，并允许医生直接手写病历后调 `POST /`（`source=M`）保存。保存接口本身不依赖 AI，不受影响。

### 参数错误

`registerId` 为空或 `dialogue` 为空：

```json
{ "code": 400, "message": "挂号ID不能为空", "data": null }
```

## 四、数据来源说明（前端无需关心，仅供理解）

- 患者性别/年龄：后端从 `register` 表取，随对话一并发给 AI（影响归纳）。
- 疾病关联：`medical_record_disease` 多对多关联 `disease` 表；由医生在 UI 选择，AI 不参与挑疾病。
- `source` 列：`medical_record` 表新增列（v1.3 ALTER），区分 AI 草稿来源与人工录入，便于复盘统计。
