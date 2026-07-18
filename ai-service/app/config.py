"""应用配置：通过 .env 文件 / 环境变量注入。

读取优先级：进程环境变量 > ai-service/.env 文件 > 字段默认值。
env_file 用绝对路径（相对本文件定位），避免 uvicorn 从不同 CWD 启动时读不到 .env。
"""

from functools import lru_cache
from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

# ai-service/app/config.py → 上两级 = ai-service/
_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(_ENV_FILE), env_file_encoding="utf-8", extra="ignore"
    )

    # GLM
    glm_api_key: str = ""
    glm_base_url: str = "https://open.bigmodel.cn/api/paas/v4"
    glm_model: str = "glm-4-plus"
    glm_embedding_model: str = "embedding-3"

    # 内部调用密钥（与 Spring Boot 共享）
    internal_key: str = "change-me-shared-with-springboot"

    # 推理控制
    ai_timeout_seconds: float = 30.0
    ai_max_retries: int = 2
    ai_temperature: float = 0.2

    # RAG 本地知识库增强
    rag_enabled: bool = True
    rag_knowledge_dir: str = str(Path(__file__).resolve().parent.parent / "knowledge")
    rag_vector_store_dir: str = str(
        Path(__file__).resolve().parent.parent / "vector_store" / "chroma"
    )
    rag_collection_name: str = "hospital_clinical_knowledge"
    rag_rebuild_on_startup: bool = True
    rag_top_k: int = 4
    rag_chunk_chars: int = 900
    rag_chunk_overlap: int = 120
    rag_max_distance: float = 1.2

    @field_validator(
        "internal_key",
        "glm_base_url",
        "glm_model",
        "glm_embedding_model",
        "rag_knowledge_dir",
        "rag_vector_store_dir",
        "rag_collection_name",
        mode="before",
    )
    @classmethod
    def _empty_string_uses_default(cls, value, info):
        if isinstance(value, str) and not value.strip():
            return cls.model_fields[info.field_name].default
        return value


@lru_cache
def get_settings() -> Settings:
    return Settings()
