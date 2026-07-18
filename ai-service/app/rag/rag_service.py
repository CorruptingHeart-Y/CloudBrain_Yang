"""Embedding based RAG with a persistent ChromaDB vector store."""

from __future__ import annotations

import hashlib
import logging
import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Any

from app.config import get_settings

logger = logging.getLogger(__name__)

_SUPPORTED_SUFFIXES = {".md", ".txt"}
_SERVICE_ROOT = Path(__file__).resolve().parents[2]


class RagUnavailableError(RuntimeError):
    """Raised when vector retrieval cannot run, usually because embeddings are unavailable."""


@dataclass(frozen=True)
class KnowledgeChunk:
    chunk_id: str
    source: str
    title: str
    content: str
    chunk_index: int


@dataclass(frozen=True)
class RetrievalHit:
    source: str
    title: str
    content: str
    distance: float


class GlmEmbeddingClient:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._client = None
        if not self._settings.glm_api_key:
            logger.warning("未配置 GLM_API_KEY，RAG 向量化不可用")
            return
        try:
            from zhipuai import ZhipuAI

            self._client = ZhipuAI(
                api_key=self._settings.glm_api_key,
                base_url=self._settings.glm_base_url,
                timeout=self._settings.ai_timeout_seconds,
            )
        except Exception as e:
            logger.warning("zhipuai embedding 客户端初始化失败: %s", e)

    @property
    def available(self) -> bool:
        return self._client is not None

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        if not self.available:
            raise RagUnavailableError("RAG 向量化不可用：缺少 GLM_API_KEY")
        if not texts:
            return []

        resp = self._client.embeddings.create(
            model=self._settings.glm_embedding_model,
            input=texts,
        )
        data = sorted(resp.data, key=lambda item: item.index)
        return [item.embedding for item in data]

    def embed_query(self, text: str) -> list[float]:
        return self.embed_documents([text])[0]


def _resolve_path(path_value: str) -> Path:
    path = Path(path_value)
    if path.is_absolute():
        return path
    return _SERVICE_ROOT / path


def _split_long_text(text: str, chunk_chars: int, overlap: int) -> list[str]:
    if len(text) <= chunk_chars:
        return [text]

    chunks: list[str] = []
    step = max(chunk_chars - overlap, 1)
    for start in range(0, len(text), step):
        chunk = text[start : start + chunk_chars].strip()
        if chunk:
            chunks.append(chunk)
        if start + chunk_chars >= len(text):
            break
    return chunks


def _chunk_document(path: Path, chunk_chars: int, overlap: int) -> list[KnowledgeChunk]:
    raw = path.read_text(encoding="utf-8").strip()
    if not raw:
        return []

    title = path.stem
    paragraphs = [p.strip() for p in re.split(r"\n\s*\n", raw) if p.strip()]
    chunks: list[KnowledgeChunk] = []
    current: list[str] = []
    current_len = 0

    def flush() -> None:
        nonlocal current, current_len
        if not current:
            return
        content = "\n".join(current).strip()
        for piece in _split_long_text(content, chunk_chars, overlap):
            digest = hashlib.sha256(
                f"{path.name}:{len(chunks)}:{piece}".encode("utf-8")
            ).hexdigest()[:16]
            chunks.append(
                KnowledgeChunk(
                    chunk_id=f"{path.stem}-{digest}",
                    source=path.name,
                    title=title,
                    content=piece,
                    chunk_index=len(chunks),
                )
            )
        current = []
        current_len = 0

    for paragraph in paragraphs:
        next_len = current_len + len(paragraph) + 1
        if current and next_len > chunk_chars:
            flush()
        current.append(paragraph)
        current_len += len(paragraph) + 1
    flush()
    return chunks


class RagService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._embedding = GlmEmbeddingClient()
        self._chunk_count = 0
        self._vector_ready = False
        self._collection = None
        self.reload()

    @property
    def enabled(self) -> bool:
        return self._settings.rag_enabled

    @property
    def embedding_available(self) -> bool:
        return self._embedding.available

    @property
    def vector_ready(self) -> bool:
        return self._vector_ready

    @property
    def chunk_count(self) -> int:
        return self._chunk_count

    @property
    def knowledge_dir(self) -> str:
        return str(_resolve_path(self._settings.rag_knowledge_dir))

    @property
    def vector_store_dir(self) -> str:
        return str(_resolve_path(self._settings.rag_vector_store_dir))

    def _load_chunks(self) -> list[KnowledgeChunk]:
        knowledge_dir = _resolve_path(self._settings.rag_knowledge_dir)
        if not knowledge_dir.exists():
            logger.warning("RAG 知识库目录不存在: %s", knowledge_dir)
            return []

        chunks: list[KnowledgeChunk] = []
        for path in sorted(knowledge_dir.rglob("*")):
            if path.is_file() and path.suffix.lower() in _SUPPORTED_SUFFIXES:
                try:
                    chunks.extend(
                        _chunk_document(
                            path,
                            self._settings.rag_chunk_chars,
                            self._settings.rag_chunk_overlap,
                        )
                    )
                except UnicodeDecodeError:
                    logger.warning("RAG 知识文件不是 UTF-8，已跳过: %s", path)
        return chunks

    def _get_collection(self) -> Any:
        if self._collection is not None:
            return self._collection

        import chromadb

        vector_dir = _resolve_path(self._settings.rag_vector_store_dir)
        vector_dir.mkdir(parents=True, exist_ok=True)
        client = chromadb.PersistentClient(path=str(vector_dir))
        self._collection = client.get_or_create_collection(
            name=self._settings.rag_collection_name,
            metadata={"hnsw:space": "cosine"},
        )
        return self._collection

    def reload(self) -> None:
        self._chunk_count = 0
        self._vector_ready = False

        if not self._settings.rag_enabled:
            logger.info("RAG 已关闭")
            return
        if not self._embedding.available:
            logger.warning("RAG 已启用，但 embedding 客户端不可用，跳过向量库构建")
            return

        chunks = self._load_chunks()
        self._chunk_count = len(chunks)
        if not chunks:
            logger.warning("RAG 知识库为空，跳过向量库构建")
            return

        collection = self._get_collection()
        if self._settings.rag_rebuild_on_startup:
            existing = collection.get()
            existing_ids = existing.get("ids") or []
            if existing_ids:
                collection.delete(ids=existing_ids)

        if collection.count() == 0:
            texts = [chunk.content for chunk in chunks]
            embeddings = self._embedding.embed_documents(texts)
            collection.add(
                ids=[chunk.chunk_id for chunk in chunks],
                documents=texts,
                embeddings=embeddings,
                metadatas=[
                    {
                        "source": chunk.source,
                        "title": chunk.title,
                        "chunk_index": chunk.chunk_index,
                    }
                    for chunk in chunks
                ],
            )

        self._chunk_count = collection.count()
        self._vector_ready = self._chunk_count > 0
        logger.info(
            "RAG 向量库就绪 | 知识目录=%s | 向量库=%s | collection=%s | 片段数=%d",
            self.knowledge_dir,
            self.vector_store_dir,
            self._settings.rag_collection_name,
            self._chunk_count,
        )

    def retrieve(self, query: str, top_k: int | None = None) -> list[RetrievalHit]:
        if not self.enabled or not self._vector_ready:
            return []
        if not query.strip():
            return []

        query_embedding = self._embedding.embed_query(query)
        collection = self._get_collection()
        limit = top_k if top_k is not None else self._settings.rag_top_k
        result = collection.query(
            query_embeddings=[query_embedding],
            n_results=limit,
            include=["documents", "metadatas", "distances"],
        )

        documents = result.get("documents", [[]])[0]
        metadatas = result.get("metadatas", [[]])[0]
        distances = result.get("distances", [[]])[0]

        hits: list[RetrievalHit] = []
        for document, metadata, distance in zip(documents, metadatas, distances):
            if distance is not None and distance > self._settings.rag_max_distance:
                continue
            metadata = metadata or {}
            hits.append(
                RetrievalHit(
                    source=str(metadata.get("source", "")),
                    title=str(metadata.get("title", "")),
                    content=document,
                    distance=float(distance or 0.0),
                )
            )
        return hits

    def build_context(self, query: str, top_k: int | None = None) -> str:
        try:
            hits = self.retrieve(query, top_k=top_k)
        except RagUnavailableError as e:
            logger.warning("RAG 检索跳过: %s", e)
            return ""

        if not hits:
            return ""

        lines: list[str] = []
        for index, hit in enumerate(hits, start=1):
            lines.append(
                f"[{index}] 来源: {hit.source} | 标题: {hit.title} | 向量距离: {hit.distance:.4f}\n"
                f"{hit.content}"
            )
        return "\n\n".join(lines)


@lru_cache
def get_rag_service() -> RagService:
    return RagService()

