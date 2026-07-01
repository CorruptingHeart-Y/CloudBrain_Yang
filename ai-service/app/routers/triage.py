"""分诊路由：POST /ai/triage。"""

from fastapi import APIRouter, HTTPException

from app.clients.glm_client import AiInferenceError, get_glm_client
from app.schemas.triage import TriageRequest, TriageResult
from app.services import triage_service

router = APIRouter(tags=["triage"])


@router.post("/triage", response_model=TriageResult)
def run_triage(req: TriageRequest) -> TriageResult:
    try:
        return triage_service.triage(req, get_glm_client())
    except AiInferenceError as e:
        raise HTTPException(
            status_code=503,
            detail=f"AI 分诊服务暂不可用，请人工分诊: {e}",
        )
