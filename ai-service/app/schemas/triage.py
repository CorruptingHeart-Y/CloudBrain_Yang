"""分诊 Pydantic 模型。

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


class CandidateDept(CameledModel):
    dept_id: int = Field(..., description="科室ID")
    dept_name: str
    dept_type: str | None = None


class CandidateDoctor(CameledModel):
    employee_id: int = Field(..., description="医生员工ID")
    dept_id: int
    realname: str
    regist_level_name: str | None = None


class TriageRequest(CameledModel):
    chief_complaint: str
    patient: PatientBrief | None = None
    departments: list[CandidateDept]
    doctors: list[CandidateDoctor]


class RecommendedDept(CameledModel):
    dept_id: int
    reason: str
    score: float | None = None


class RecommendedDoctor(CameledModel):
    employee_id: int
    dept_id: int
    reason: str
    score: float | None = None


class TriageResult(CameledModel):
    departments: list[RecommendedDept]
    doctors: list[RecommendedDoctor]
