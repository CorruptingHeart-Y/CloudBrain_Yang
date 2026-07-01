"""GLM 客户端封装：结构化 JSON 输出 + 超时 + 重试 + 异常降级。

设计要点：
- 调用 GLM 的 JSON 模式（response_format=json_object）强制结构化输出，再用 Pydantic 校验。
- 解析失败 / 调用异常按 ai_max_retries 重试，全部失败抛 AiInferenceError，由路由层转 503 降级。
- 未配置 GLM_API_KEY 时 available=False，调用直接降级，不致崩。
"""

import json
import logging
from typing import Type, TypeVar

from pydantic import BaseModel, ValidationError

from app.config import get_settings

logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseModel)


class AiInferenceError(RuntimeError):
    """AI 推理失败（无 Key / 超时 / 解析失败），上层据此降级。"""


class GlmClient:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._client = None
        if not self._settings.glm_api_key:
            logger.warning("未配置 GLM_API_KEY，AI 推理将直接降级")
            return
        try:
            from zhipuai import ZhipuAI

            self._client = ZhipuAI(
                api_key=self._settings.glm_api_key,
                base_url=self._settings.glm_base_url,
                timeout=self._settings.ai_timeout_seconds,
            )
        except Exception as e:  # SDK 缺失 / 初始化异常
            logger.warning("zhipuai 客户端初始化失败: %s", e)

    @property
    def available(self) -> bool:
        return self._client is not None

    def structured_complete(
        self, system_prompt: str, user_content: str, schema_model: Type[T]
    ) -> T:
        """调用 GLM 并将返回校验为 schema_model。失败抛 AiInferenceError。"""
        if not self.available:
            raise AiInferenceError("AI 服务未配置（缺少 GLM_API_KEY）")

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content},
        ]
        last_err: Exception | None = None
        for attempt in range(self._settings.ai_max_retries + 1):
            try:
                resp = self._client.chat.completions.create(
                    model=self._settings.glm_model,
                    messages=messages,
                    response_format={"type": "json_object"},
                    temperature=self._settings.ai_temperature,
                )
                content = resp.choices[0].message.content
                data = json.loads(content)
                return schema_model.model_validate(data)
            except (ValidationError, json.JSONDecodeError) as e:
                last_err = e
                logger.warning("结构化解析失败（第 %d 次）: %s", attempt + 1, e)
            except Exception as e:
                last_err = e
                logger.warning("GLM 调用异常（第 %d 次）: %s", attempt + 1, e)
        raise AiInferenceError(f"AI 推理失败: {last_err}")


_glm: GlmClient | None = None


def get_glm_client() -> GlmClient:
    """单例，首次调用时初始化。"""
    global _glm
    if _glm is None:
        _glm = GlmClient()
    return _glm
