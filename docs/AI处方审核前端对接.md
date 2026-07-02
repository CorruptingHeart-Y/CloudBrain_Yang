# AI 处方审核 — 前端对接说明

> 后端模块：`com.neusoft.hospital.ai`（PrescriptionAuditController）
> Python 推理端点：`POST /ai/prescription-check`（前端不直接调，由 Java 网关转发）
> 鉴权：所有接口需 `Authorization: Bearer <JWT>`（员工登录后获取）

## 一、接口清单

基地址：`/api/v1/prescription`

| 方法 | 路径 | 用途 | 是否落库 |
|---|---|---|---|
| POST | `/check` | 处方审核**预览** | 否 |
| POST | `/check/confirm` | 处方审核**并落库** | 是 → `prescription_audit_record` |
| GET  | `/check/record?registerId=` | 查询某挂号的审核留痕记录 | — |

## 二、调用时机（重要）

本系统处方是**一行一药**模型（`prescription` 表，同一 `registerId` 多行 = 一个处方）。AI 审核针对**整个处方集合**，不是单行。因此：

- 医生开药过程中，每加一味药调 `POST /api/v1/prescription` 存一行（已有 CRUD，不变）。
- 医生**开完该挂号全部药品后**，由前端主动调一次 `POST /check`（预览看风险）或 `POST /check/confirm`（预览+留痕）。
- **后端不会在保存单行处方时自动触发审核**——单行审核无意义。`/check/confirm` 是"处方定稿审核"的显式动作。

> 建议交互：医生点「AI 审核」按钮 → 调 `/check` 预览展示风险；医生点「确认开方」→ 调 `/check/confirm` 落库留痕，再走发药流程。

## 三、请求 / 响应

### POST /check、POST /check/confirm

请求体（两者一致）：

```json
{ "registerId": 14 }
```

响应（两者一致，`code=200` 即成功）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "riskLevel": "medium",
    "suggestions": [
      { "drugId": 4, "drugName": "硝苯地平控释片", "content": "建议早晨服用，避免睡前服用以减少夜间低血压风险" }
    ],
    "interactions": [
      { "drugA": 4, "drugAName": "硝苯地平控释片", "drugB": 15, "drugBName": "舒血宁注射液", "level": "medium", "desc": "两者合用可能增强降压效果，需监测血压" }
    ],
    "riskItems": [
      { "drugId": 15, "drugName": "舒血宁注射液", "type": "剂量", "desc": "静脉滴注需缓慢，密切监测反应" }
    ]
  }
}
```

字段说明：
- `riskLevel`：总体风险等级，`low` / `medium` / `high`。
- `suggestions`：用药建议（可按 drugId 关联到具体药；`drugId` 可能为 null 表示整体建议）。
- `interactions`：两两药物相互作用，`level` 同为 low/medium/high。
- `riskItems`：单药风险项，`type` 如「剂量」「禁忌」「过敏」。
- 三个数组任一无内容时返回 `[]`，不会缺键。

### 降级（AI 不可用）

AI 服务超时/不可达时返回 503，**不阻塞开方**：

```json
{ "code": 503, "message": "AI处方审核服务暂不可用，请人工核对", "data": null }
```

前端此时应提示"AI 审核不可用，请人工核对"，并允许医生继续开方（处方数据已保存，不受影响）。

### 参数错误

`registerId` 为空或该挂号下无处方明细：

```json
{ "code": 400, "message": "该挂号下无处方明细，无法审核", "data": null }
```

### GET /check/record?registerId=14

返回该挂号的审核留痕（按时间倒序）：

```json
{
  "code": 200,
  "data": [
    {
      "id": 3,
      "registerId": 14,
      "riskLevel": "medium",
      "auditorEmployeeId": 1,
      "creationTime": "2026-07-01T19:30:00",
      "requestSnapshot": "{\"registerId\":14,\"patient\":{...},\"drugs\":[...]}",
      "resultJson": "{\"riskLevel\":\"medium\",\"suggestions\":[...]}"
    }
  ]
}
```

- `requestSnapshot` / `resultJson` 是落库时的 **JSON 字符串**，前端按需 `JSON.parse` 后展示。
- 同一挂号多次 `confirm` 会产生多条记录（每次审核都留痕）。

## 四、数据来源说明（前端无需关心，仅供理解）

- 药品明细：按 `registerId` 从 `prescription` 表聚合全部行。
- 药名/规格：后端用 `drug_info` 表富化，响应里 `drugName` 由后端补，AI 只返回 `drugId`。
- 患者年龄/性别：后端从 `register` 表取，一并发给 AI（影响剂量判断）。
