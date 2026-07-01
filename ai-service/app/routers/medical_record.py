"""病历生成路由：POST /ai/medical-record。"""

from fastapi import APIRouter, HTTPException

from app.clients.glm_client import AiInferenceError, get_glm_client
from app.schemas.medical_record import MedicalRecordDraft, MedicalRecordRequest
from app.services import medical_record_service

router = APIRouter(tags=["medical-record"])


@router.post("/medical-record", response_model=MedicalRecordDraft)
def generate_medical_record(req: MedicalRecordRequest) -> MedicalRecordDraft:
    try:
        return medical_record_service.generate(req, get_glm_client())
    except AiInferenceError as e:
        raise HTTPException(
            status_code=503,
            detail=f"AI 病历生成服务暂不可用，请人工书写: {e}",
        )
