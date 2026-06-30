"""FastAPI 入口：X-Internal-Key 校验中间件 + 路由装配。"""

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.clients.glm_client import get_glm_client
from app.config import get_settings
from app.routers import triage as triage_router

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s"
)

settings = get_settings()

app = FastAPI(title="Hospital AI Service", version="0.1.0")

# 无需内部密钥的公开路径
_PUBLIC_PREFIXES = ("/docs", "/redoc", "/openapi", "/health")


class InternalKeyMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        path = request.url.path
        if path == "/health" or any(path.startswith(p) for p in _PUBLIC_PREFIXES):
            return await call_next(request)
        if request.headers.get("X-Internal-Key") != settings.internal_key:
            return JSONResponse(status_code=401, content={"detail": "invalid internal key"})
        return await call_next(request)


app.add_middleware(InternalKeyMiddleware)


@app.get("/health")
def health():
    return {"status": "up", "glm_configured": get_glm_client().available}


app.include_router(triage_router.router, prefix="/ai")
