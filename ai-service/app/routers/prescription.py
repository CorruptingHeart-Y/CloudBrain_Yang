"""处方审核路由：POST /ai/prescription-check。"""

from fastapi import APIRouter, HTTPException

from app.clients.glm_client import AiInferenceError, get_glm_client
from app.schemas.prescription import PrescriptionCheckRequest, PrescriptionCheckResult
from app.services import prescription_service

router = APIRouter(tags=["prescription"])


@router.post("/prescription-check", response_model=PrescriptionCheckResult)
def check_prescription(req: PrescriptionCheckRequest) -> PrescriptionCheckResult:
    try:
        return prescription_service.check(req, get_glm_client())
    except AiInferenceError as e:
        raise HTTPException(
            status_code=503,
            detail=f"AI 处方审核服务暂不可用，请人工核对: {e}",
        )
