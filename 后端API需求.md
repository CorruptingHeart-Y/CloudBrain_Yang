# 后端 API 需求（角色切换登录）

## 背景

前端已实现角色切换登录（患者 / 医生 / 管理员），但目前角色存储在前端 localStorage 中，后端不感知。为确保安全性，需要后端配合以下改动。

---

## 1. 用户角色字段

### 1.1 数据模型

`employee`（或 `user`）表新增 `role` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `role` | varchar(20) | 角色枚举：`PATIENT` / `DOCTOR` / `ADMIN` |

### 1.2 登录响应

`POST /api/v1/auth/login` 响应的 `userInfo` 中增加 `role` 字段：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJ...",
    "expiresAt": 1700000000000,
    "userInfo": {
      "id": 1,
      "realname": "李医生",
      "deptmentId": 1,
      "deptName": "神经内科",
      "registLevelId": 1,
      "registLevelName": "普通号",
      "role": "DOCTOR"
    }
  }
}
```

### 1.3 当前用户接口

`GET /api/v1/auth/me` 响应中同样增加 `role` 字段。

---

## 2. JWT Token

JWT payload 中嵌入 `role` claim：

```json
{
  "sub": "1",
  "realname": "李医生",
  "role": "DOCTOR",
  "iat": 1700000000,
  "exp": 1700028800
}
```

---

## 3. 后端接口鉴权

按角色对接口做访问控制（建议通过拦截器/中间件实现）：

| 接口前缀 | 允许角色 |
|----------|----------|
| `/api/v1/auth/**` | 所有角色（含未登录） |
| `/api/v1/dashboard` (如有) | 所有已登录角色 |
| `/api/v1/register/**` | ADMIN, DOCTOR, PATIENT（患者仅可查看自己的） |
| `/api/v1/employee/**` | ADMIN |
| `/api/v1/department/**` | ADMIN |
| `/api/v1/scheduling/**` | ADMIN |
| `/api/v1/regist-level/**` | ADMIN |
| `/api/v1/settle-category/**` | ADMIN |
| `/api/v1/medical-technology/**` | ADMIN, DOCTOR |
| `/api/v1/drug-info/**` | ADMIN, DOCTOR |
| `/api/v1/disease/**` | ADMIN, DOCTOR |
| `/api/v1/check-request/**` | ADMIN, DOCTOR |
| `/api/v1/inspection-request/**` | ADMIN, DOCTOR |
| `/api/v1/disposal-request/**` | ADMIN, DOCTOR |
| `/api/v1/prescription/**` | ADMIN, DOCTOR |

无权限时返回 `403 Forbidden`（而非 401）。

---

## 4. 患者个人记录查询（可选）

新增接口，供患者查询自己相关的医疗记录：

```
GET /api/v1/patient/records?employeeId={id}
```

返回该员工对应的挂号记录、处方、检查检验结果等。

---

## 5. 前端兼容性说明

当前前端在登录时由用户选择角色，角色存储在 `localStorage` 的 `role` 键中。后端实现上述改动后：

1. 前端可改为从登录响应的 `userInfo.role` 中读取角色，移除手动选择
2. 前端可在请求头中携带 `X-Role` 供后端日志记录
3. 路由守卫和侧边栏过滤继续从 `authStore.role` 读取
