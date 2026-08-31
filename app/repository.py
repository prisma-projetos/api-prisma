from datetime import datetime, timezone

from .schemas import Analysis, Article, ClaimType, Evidence, Narrative, Perspective


class InMemoryRepository:
    def __init__(self) -> None:
        self._articles = [
            Article(
                id="article-001",
                title="Programa econômico de Silva divide analistas",
                source_name="Jornal Exemplo",
                author="Ana Costa",
                published_at=datetime(2026, 8, 20, tzinfo=timezone.utc),
                url="https://example.com/article-001",
                entities=["politico-silva", "partido-alpha"],
                perspective=Perspective.center_left,
                classification_confidence=0.82,
                content_hash="hash-001",
            ),
            Article(
                id="article-002",
                title="Partido Alpha defende agenda de reformas",
                source_name="Notícias Agora",
                published_at=datetime(2026, 8, 21, tzinfo=timezone.utc),
                url="https://example.com/article-002",
                entities=["partido-alpha"],
                perspective=Perspective.center_right,
                classification_confidence=0.76,
                content_hash="hash-002",
            ),
        ]

    def list_articles(
        self,
        entity: str | None = None,
        perspective: Perspective | None = None,
        limit: int | None = None,
        offset: int = 0,
    ) -> tuple[list[Article], int]:
        result = self._articles
        if entity:
            result = [article for article in result if entity.lower() in [item.lower() for item in article.entities]]
        if perspective:
            result = [article for article in result if article.perspective == perspective]
        total = len(result)
        if limit is not None:
            result = result[offset : offset + limit]
        return result, total

    def list_analyses(
        self, entity: str | None = None, perspective: Perspective | None = None
    ) -> list[Analysis]:
        articles, _ = self.list_articles(entity, perspective)
        entities = [entity] if entity else sorted({item for article in articles for item in article.entities})
        analyses: list[Analysis] = []
        for current_entity in entities:
            entity_articles = [article for article in articles if current_entity in article.entities]
            perspectives = sorted({article.perspective for article in entity_articles}, key=lambda item: item.value)
            for current_perspective in perspectives:
                perspective_articles = [
                    article for article in entity_articles if article.perspective == current_perspective
                ]
                evidence = [
                    Evidence(
                        article_id=article.id,
                        source_name=article.source_name,
                        title=article.title,
                        url=article.url,
                        published_at=article.published_at,
                        excerpt="Trecho armazenado para rastrear a narrativa até a fonte original.",
                        claim_type=ClaimType.opinion,
                    )
                    for article in perspective_articles
                ]
                analyses.append(
                    Analysis(
                        entity=current_entity,
                        perspective=current_perspective,
                        article_count=len(perspective_articles),
                        model_version="desenvolvimento-v1",
                        generated_at=datetime.now(timezone.utc),
                        narratives=[
                            Narrative(
                                text="A cobertura desta perspectiva enfatiza os argumentos presentes nos artigos analisados.",
                                claim_type=ClaimType.inference,
                                confidence=0.68,
                                evidence=evidence,
                            )
                        ],
                    )
                )
        return analyses
