from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field, HttpUrl


class Perspective(str, Enum):
    left = "left"
    center_left = "center-left"
    center = "center"
    center_right = "center-right"
    right = "right"
    unknown = "unknown"


class ClaimType(str, Enum):
    fact = "fact"
    opinion = "opinion"
    inference = "inference"


class Evidence(BaseModel):
    article_id: str
    source_name: str
    title: str
    url: HttpUrl
    published_at: datetime
    excerpt: str
    claim_type: ClaimType


class Narrative(BaseModel):
    text: str
    claim_type: ClaimType
    confidence: float = Field(ge=0, le=1)
    evidence: list[Evidence]


class Article(BaseModel):
    id: str
    title: str
    source_name: str
    author: str | None = None
    published_at: datetime
    url: HttpUrl
    entities: list[str]
    perspective: Perspective
    classification_confidence: float = Field(ge=0, le=1)
    content_hash: str
    duplicate_of: str | None = None


class Analysis(BaseModel):
    entity: str
    perspective: Perspective
    article_count: int
    model_version: str
    generated_at: datetime
    narratives: list[Narrative]


class AnalysisList(BaseModel):
    items: list[Analysis]
    total: int


class ArticleList(BaseModel):
    items: list[Article]
    total: int
