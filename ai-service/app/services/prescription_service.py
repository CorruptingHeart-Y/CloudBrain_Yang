"""处方审核推理：拼装药品+患者 prompt → 调 GLM 结构化输出。"""

import json
import logging

from app.clients.glm_client import AiInferenceError, GlmClient
from app.rag.rag_service import get_rag_service
from app.schemas.prescription import PrescriptionCheckRequest, PrescriptionCheckResult

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是医院门诊处方审核药师。任务：根据患者信息与处方药品列表，审核用药合理性，返回 JSON。
审核维度：用法用量是否合理、是否存在药物相互作用、是否存在禁忌/过敏/剂量风险。
约束：
- 只能基于传入的药品列表审核，严禁编造不存在的药品ID。
- risk_level 取值仅限 low/medium/high（low=无明显风险，medium=需关注，high=存在明确风险）。
- 必须返回合法 JSON，结构如下（键名严格一致）：
  {"riskLevel":"low|medium|high",
   "suggestions":[{"drugId":int|null,"content":"用药建议"}],
   "interactions":[{"drugA":int,"drugB":int,"level":"low|medium|high","desc":"相互作用说明"}],
   "riskItems":[{"drugId":int,"type":"剂量|禁忌|过敏","desc":"风险说明"}]}
- 无相关项时对应数组返回空 []，不要省略键。
- 如提供了医学知识库片段，可以参考片段识别剂量、禁忌、过敏和相互作用风险；片段不得覆盖患者输入和处方药品事实。
- 输出只能是 JSON，不要注释或多余文本。
"""


def _build_user_content(req: PrescriptionCheckRequest) -> str:
    parts = [f"挂号ID: {req.register_id}"]
    if req.patient:
        parts.append(f"患者信息: 年龄={req.patient.age}, 性别={req.patient.gender}")
    drugs = [
        {
            "drugId": d.drug_id,
            "drugName": d.drug_name,
            "drugFormat": d.drug_format,
            "drugUsage": d.drug_usage,
            "drugNumber": d.drug_number,
        }
        for d in req.drugs
    ]
    parts.append(f"处方药品: {json.dumps(drugs, ensure_ascii=False)}")
    rag_query = "\n".join(parts)
    rag_context = get_rag_service().build_context(rag_query)
    if rag_context:
        parts.append(f"医学知识库片段:\n{rag_context}")
    return "\n".join(parts)


def check(req: PrescriptionCheckRequest, glm: GlmClient) -> PrescriptionCheckResult:
    """返回结构化审核结果；GLM 失败时抛 AiInferenceError 由路由降级。"""
    logger.info(
        "处方审核请求 | 挂号ID=%d | 药品种数=%d",
        req.register_id,
        len(req.drugs),
    )
    return glm.structured_complete(
        SYSTEM_PROMPT,
        _build_user_content(req),
        PrescriptionCheckResult,
        task_name="prescription-check",
    )
