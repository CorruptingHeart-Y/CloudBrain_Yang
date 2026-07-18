# Hospital AI Service

医院 AI 推理服务（FastAPI + GLM），供 Spring Boot 后端通过内部密钥调用。三个结构化任务：诊前分诊 / 处方审核 / 病历生成。业务数据不碰 MySQL/Redis；RAG 知识向量落本地 ChromaDB。

## 依赖管理

使用 [uv](https://docs.astral.sh/uv/)。

## 本地运行

```bash
cd ai-service
cp .env.example .env      # 填入 GLM_API_KEY，以及与 Spring Boot 一致的 INTERNAL_KEY
uv sync                   # 安装依赖
uv run uvicorn app.main:app --reload --port 8000
```

- 健康检查：`GET http://localhost:8000/health`（公开，返回 GLM 是否已配置）
- 健康检查也会返回 RAG 开关、Embedding 客户端、向量库和知识片段状态。
- 重建 RAG：`POST http://localhost:8000/ai/rag/reload`，请求头需带 `X-Internal-Key`
- API 文档：`http://localhost:8000/docs`（公开）
- 分诊：`POST http://localhost:8000/ai/triage`，请求头需带 `X-Internal-Key`

## 分诊请求示例

```json
{
  "chiefComplaint": "头疼三天，伴有恶心",
  "patient": { "age": 45, "gender": "男" },
  "departments": [
    { "deptId": 3, "deptName": "神经内科", "deptType": "内科" },
    { "deptId": 7, "deptName": "神经外科", "deptType": "外科" }
  ],
  "doctors": [
    { "employeeId": 12, "deptId": 3, "realname": "张医生", "registLevelName": "专家" }
  ]
}
```

返回（GLM 在候选集内选+排序，返回 ID + 理由）：

```json
{
  "departments": [{ "deptId": 3, "reason": "...", "score": 0.9 }],
  "doctors": [{ "employeeId": 12, "deptId": 3, "reason": "...", "score": 0.85 }]
}
```

## 降级

未配置 `GLM_API_KEY` 或 GLM 调用/解析失败时，`/ai/triage` 返回 HTTP 503 与友好提示，不致崩溃；Spring Boot 侧据此向前端返回"请人工分诊"。

## RAG 本地知识库

服务启动时会读取 `ai-service/knowledge` 下的 `.md` / `.txt` 文件，按段落和长度进行 chunking，调用 `GLM_EMBEDDING_MODEL` 生成向量，并写入本地持久化 ChromaDB 向量库。分诊、处方审核、病历生成请求会先把患者输入和业务数据向量化，再从 ChromaDB 中按余弦距离召回 top-k 片段，把命中的医学参考资料注入 GLM prompt；接口入参与出参不变。

可用环境变量：

```bash
GLM_EMBEDDING_MODEL=embedding-3
RAG_ENABLED=true
RAG_KNOWLEDGE_DIR=./knowledge
RAG_VECTOR_STORE_DIR=./vector_store/chroma
RAG_COLLECTION_NAME=hospital_clinical_knowledge
RAG_REBUILD_ON_STARTUP=true
RAG_TOP_K=4
RAG_CHUNK_CHARS=900
RAG_CHUNK_OVERLAP=120
RAG_MAX_DISTANCE=1.2
```

更新知识库后可以重启服务，或调用 `/ai/rag/reload` 重新构建向量库。`RAG_REBUILD_ON_STARTUP=true` 适合开发阶段；生产环境可改为 `false`，配合离线构建脚本或管理接口重建索引。知识库内容仅作为辅助参考，模型仍必须优先遵循接口传入的患者事实、候选集和处方清单。

## Docker

随仓库根 `docker-compose.yml` 的 `ai-service` 服务一起启动。
