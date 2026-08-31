from enum import Enum
import os

from fastapi import FastAPI, Path, Query, Request
from fastapi.responses import RedirectResponse, Response

from .repository import InMemoryRepository
from .schemas import AnalysisList, ArticleList, Perspective

app = FastAPI(
    title="Análise de Viés Político",
    version="0.1.0",
    description="Consulta conteúdos classificados e sínteses rastreáveis de fontes jornalísticas.",
    docs_url=None if os.getenv("ENVIRONMENT", "development") == "production" else "/docs",
    redoc_url=None if os.getenv("ENVIRONMENT", "development") == "production" else "/redoc",
)
repository = InMemoryRepository()


@app.get("/", include_in_schema=False)
def home() -> RedirectResponse:
    return RedirectResponse(url="/docs", status_code=307)


@app.middleware("http")
async def security_headers(request: Request, call_next) -> Response:
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Content-Security-Policy"] = "default-src 'self'; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data:; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'"
    response.headers["Permissions-Policy"] = "camera=(), microphone=(), geolocation=()"
    if request.url.scheme == "https":
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    return response


class PerspectiveQuery(str, Enum):
    left = "left"
    center_left = "center-left"
    center = "center"
    center_right = "center-right"
    right = "right"
    unknown = "unknown"


def to_perspective(value: PerspectiveQuery | None) -> Perspective | None:
    return Perspective(value.value) if value else None


@app.get("/health", tags=["sistema"])
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/articles", response_model=ArticleList, tags=["artigos"])
def get_articles(
    entity: str | None = Query(
        default=None,
        min_length=1,
        max_length=100,
        description="Identificador do político ou partido.",
    ),
    perspective: PerspectiveQuery | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0, le=100_000),
) -> ArticleList:
    items, total = repository.list_articles(entity, to_perspective(perspective), limit, offset)
    return ArticleList(items=items, total=total)


@app.get("/analyses/{entity_id}", response_model=AnalysisList, tags=["análises"])
def get_analyses(
    entity_id: str = Path(
        min_length=1,
        max_length=100,
        description="Identificador do político, partido ou candidato.",
    ),
    perspective: PerspectiveQuery | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0, le=100_000),
) -> AnalysisList:
    items = repository.list_analyses(entity_id, to_perspective(perspective))
    return AnalysisList(items=items[offset : offset + limit], total=len(items))
