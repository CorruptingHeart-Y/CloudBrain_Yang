"""GLM 客户端封装：结构化 JSON 输出 + 超时 + 重试 + 异常降级。

设计要点：
- 调用 GLM 的 JSON 模式（response_format=json_object）强制结构化输出，再用 Pydantic 校验。
- 解析失败 / 调用异常按 ai_max_retries 重试，全部失败抛 AiInferenceError，由路由层转 503 降级。
- 未配置 GLM_API_KEY 时 available=False，调用直接降级，不致崩。
"""

import json
import logging
import time
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
        self,
        system_prompt: str,
        user_content: str,
        schema_model: Type[T],
        task_name: str = "unknown",
    ) -> T:
        """调用 GLM 并将返回校验为 schema_model。失败抛 AiInferenceError。"""
        if not self.available:
            raise AiInferenceError("AI 服务未配置（缺少 GLM_API_KEY）")

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content},
        ]

        logger.info(
            "AI 调用开始 | 任务=%s | 模型=%s | system_prompt_len=%d | user_content_len=%d | 最大重试=%d",
            task_name,
            self._settings.glm_model,
            len(system_prompt),
            len(user_content),
            self._settings.ai_max_retries,
        )

        last_err: Exception | None = None
        t_start = time.monotonic()
        for attempt in range(self._settings.ai_max_retries + 1):
            try:
                resp = self._client.chat.completions.create(
                    model=self._settings.glm_model,
                    messages=messages,
                    response_format={"type": "json_object"},
                    temperature=self._settings.ai_temperature,
                )
                elapsed_ms = (time.monotonic() - t_start) * 1000

                content = resp.choices[0].message.content
                usage = resp.usage
                usage_str = (
                    f"prompt_tokens={usage.prompt_tokens} "
                    f"completion_tokens={usage.completion_tokens} "
                    f"total_tokens={usage.total_tokens}"
                    if usage
                    else "token_usage=N/A"
                )

                logger.info(
                    "AI 调用成功 | 任务=%s | 耗时=%.0fms | 尝试=%d/%d | %s",
                    task_name,
                    elapsed_ms,
                    attempt + 1,
                    self._settings.ai_max_retries + 1,
                    usage_str,
                )

                data = json.loads(content)
                result = schema_model.model_validate(data)
                logger.info(
                    "AI 结果校验通过 | 任务=%s | schema=%s",
                    task_name,
                    schema_model.__name__,
                )
                return result
            except (ValidationError, json.JSONDecodeError) as e:
                last_err = e
                logger.warning(
                    "AI 结构化解析失败 | 任务=%s | 尝试=%d/%d | 错误=%s",
                    task_name,
                    attempt + 1,
                    self._settings.ai_max_retries + 1,
                    e,
                )
            except Exception as e:
                last_err = e
                logger.warning(
                    "AI 调用异常 | 任务=%s | 尝试=%d/%d | 错误=%s",
                    task_name,
                    attempt + 1,
                    self._settings.ai_max_retries + 1,
                    e,
                )

        elapsed_ms = (time.monotonic() - t_start) * 1000
        logger.error(
            "AI 调用最终失败 | 任务=%s | 总耗时=%.0fms | 总尝试=%d | 最后错误=%s",
            task_name,
            elapsed_ms,
            self._settings.ai_max_retries + 1,
            last_err,
        )
        raise AiInferenceError(f"AI 推理失败: {last_err}")


_glm: GlmClient | None = None


def get_glm_client() -> GlmClient:
    """单例，首次调用时初始化。"""
    global _glm
    if _glm is None:
        _glm = GlmClient()
    return _glm
