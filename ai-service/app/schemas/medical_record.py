"""病历生成 Pydantic 模型。

字段在 Python 内用 snake_case，JSON 用 camelCase（alias_generator），对齐 Spring Boot/Jackson。
GLM 被要求输出 camelCase 键，与响应模型一致。
"""

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class CameledModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class PatientBrief(CameledModel):
    age: int | None = None
    gender: str | None = None


class MedicalRecordRequest(CameledModel):
    register_id: int = Field(..., description="挂号ID")
    patient: PatientBrief | None = None
    dialogue: str = Field(..., description="医患对话文本，医生手输/粘贴")


class MedicalRecordDraft(CameledModel):
    """AI 生成的门诊病历草稿，9 个临床字段，对齐 medical_record 表。"""

    readme: str | None = Field(None, description="主诉")
    present: str | None = Field(None, description="现病史")
    present_treat: str | None = Field(None, description="现病治疗情况")
    history: str | None = Field(None, description="既往史")
    allergy: str | None = Field(None, description="过敏史")
    physique: str | None = Field(None, description="体格检查")
    proposal: str | None = Field(None, description="检查/检验建议")
    careful: str | None = Field(None, description="注意事项")
    diagnosis: str | None = Field(None, description="诊断结果")
    cure: str | None = Field(None, description="处理意见")
