"""应用配置：通过 .env 文件 / 环境变量注入。

读取优先级：进程环境变量 > ai-service/.env 文件 > 字段默认值。
env_file 用绝对路径（相对本文件定位），避免 uvicorn 从不同 CWD 启动时读不到 .env。
"""

from functools import lru_cache
from pathlib import Path

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

    # 内部调用密钥（与 Spring Boot 共享）
    internal_key: str = "change-me-shared-with-springboot"

    # 推理控制
    ai_timeout_seconds: float = 30.0
    ai_max_retries: int = 2
    ai_temperature: float = 0.2


@lru_cache
def get_settings() -> Settings:
    return Settings()
