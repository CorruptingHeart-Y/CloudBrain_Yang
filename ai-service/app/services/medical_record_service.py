"""病历生成推理：拼装对话+患者 prompt → 调 GLM 结构化输出 9 临床字段。"""

import logging

from app.clients.glm_client import AiInferenceError, GlmClient
from app.schemas.medical_record import MedicalRecordDraft, MedicalRecordRequest

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是医院门诊病历书写助手。任务：根据医生提供的医患对话文本，提取并归纳生成结构化门诊病历草稿，返回 JSON。
字段含义（键名严格一致）：
  "readme": 主诉（患者就诊主要症状+持续时间，精炼一句话）
  "present": 现病史（本次发病经过、症状演变）
  "presentTreat": 现病治疗情况（来诊前是否用药/处置）
  "history": 既往史（既往疾病/手术史，无则空串）
  "allergy": 过敏史（药物/食物过敏，无则填"无"）
  "physique": 体格检查（体温/血压/心肺腹等阳性体征，对话未提及则空串）
  "proposal": 检查/检验建议（如血常规、CT 等）
  "careful": 注意事项（休息/饮食/复诊等医嘱）
  "diagnosis": 诊断结果（初步诊断，可多条用顿号分隔）
  "cure": 处理意见（用药/治疗/转诊等）
约束：
- 只能基于对话文本归纳，不得编造对话中不存在的信息；未提及的字段返回空字符串 ""。
- 语言简练专业，使用医学术语，避免口语化。
- 必须返回合法 JSON，只含上述 10 个键，不要注释或多余文本。
"""


def _build_user_content(req: MedicalRecordRequest) -> str:
    parts = [f"挂号ID: {req.register_id}"]
    if req.patient:
        parts.append(f"患者信息: 年龄={req.patient.age}, 性别={req.patient.gender}")
    parts.append(f"医患对话文本:\n{req.dialogue}")
    return "\n".join(parts)


def generate(req: MedicalRecordRequest, glm: GlmClient) -> MedicalRecordDraft:
    """返回病历草稿；GLM 失败时抛 AiInferenceError 由路由降级。"""
    logger.info(
        "病历生成请求 | 挂号ID=%d | 对话长度=%d",
        req.register_id,
        len(req.dialogue),
    )
    return glm.structured_complete(
        SYSTEM_PROMPT,
        _build_user_content(req),
        MedicalRecordDraft,
        task_name="medical-record",
    )
