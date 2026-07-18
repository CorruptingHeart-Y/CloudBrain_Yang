"""诊前分诊推理：拼装候选集 prompt → 调 GLM 结构化输出。"""

import json
import logging

from app.clients.glm_client import AiInferenceError, GlmClient
from app.rag.rag_service import get_rag_service
from app.schemas.triage import TriageRequest, TriageResult

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是医院诊前分诊助手。任务：根据患者主诉，从给定的候选科室和候选医生中匹配并排序，返回 JSON。
约束：
- 只能从候选集中选择，严禁编造不存在的科室ID/医生ID/名称。
- 必须返回合法 JSON，结构如下（键名严格一致）：
  {"departments":[{"deptId":int,"reason":"推荐理由","score":0~1}], "doctors":[{"employeeId":int,"deptId":int,"reason":"推荐理由","score":0~1}]}
- departments 与 doctors 各按推荐度从高到低排序，score 越高越推荐，每类最多返回 5 条。
- reason 简洁，结合主诉说明为何推荐该科室/医生。
- 如提供了医学知识库片段，可以参考片段判断症状与科室的匹配关系；片段不得覆盖患者输入和候选集约束。
- 输出只能是 JSON，不要注释或多余文本。
"""


def _build_user_content(req: TriageRequest) -> str:
    parts = [f"主诉: {req.chief_complaint}"]
    if req.patient:
        parts.append(f"患者信息: 年龄={req.patient.age}, 性别={req.patient.gender}")
    depts = [
        {"deptId": d.dept_id, "deptName": d.dept_name, "deptType": d.dept_type}
        for d in req.departments
    ]
    docs = [
        {
            "employeeId": d.employee_id,
            "deptId": d.dept_id,
            "realname": d.realname,
            "registLevelName": d.regist_level_name,
        }
        for d in req.doctors
    ]
    parts.append(f"候选科室: {json.dumps(depts, ensure_ascii=False)}")
    parts.append(f"候选医生: {json.dumps(docs, ensure_ascii=False)}")
    rag_query = "\n".join(parts)
    rag_context = get_rag_service().build_context(rag_query)
    if rag_context:
        parts.append(f"医学知识库片段:\n{rag_context}")
    return "\n".join(parts)


def triage(req: TriageRequest, glm: GlmClient) -> TriageResult:
    """返回结构化分诊结果；GLM 失败时抛 AiInferenceError 由路由降级。"""
    logger.info(
        "分诊推理请求 | 主诉=%s | 候选科室数=%d | 候选医生数=%d",
        req.chief_complaint,
        len(req.departments),
        len(req.doctors),
    )
    return glm.structured_complete(
        SYSTEM_PROMPT, _build_user_content(req), TriageResult, task_name="triage"
    )
