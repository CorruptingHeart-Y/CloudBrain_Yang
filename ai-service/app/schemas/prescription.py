"""处方审核 Pydantic 模型。

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


class PrescriptionDrug(CameledModel):
    drug_id: int = Field(..., description="药品ID")
    drug_name: str
    drug_format: str | None = None
    drug_usage: str | None = None
    drug_number: str | None = None


class PrescriptionCheckRequest(CameledModel):
    register_id: int = Field(..., description="挂号ID")
    patient: PatientBrief | None = None
    drugs: list[PrescriptionDrug]


class Suggestion(CameledModel):
    drug_id: int | None = None
    content: str = Field(..., description="用药建议文本")


class Interaction(CameledModel):
    drug_a: int | None = None
    drug_b: int | None = None
    level: str | None = Field(None, description="风险等级：low/medium/high")
    desc: str | None = None


class RiskItem(CameledModel):
    drug_id: int | None = None
    type: str | None = Field(None, description="风险类型，如 剂量/禁忌/过敏")
    desc: str | None = None


class PrescriptionCheckResult(CameledModel):
    risk_level: str = Field(..., description="总体风险等级：low/medium/high")
    suggestions: list[Suggestion] = []
    interactions: list[Interaction] = []
    risk_items: list[RiskItem] = []
