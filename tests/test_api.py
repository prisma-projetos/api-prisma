from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
    assert response.headers["x-content-type-options"] == "nosniff"
    assert response.headers["x-frame-options"] == "DENY"


def test_documentation_is_public() -> None:
    response = client.get("/docs")
    assert response.status_code == 200
    assert "SwaggerUIBundle" in response.text


def test_analysis_keeps_evidence_and_claim_type() -> None:
    response = client.get("/analyses/partido-alpha")
    assert response.status_code == 200
    payload = response.json()
    assert payload["total"] == 2
    narrative = payload["items"][0]["narratives"][0]
    assert narrative["claim_type"] == "inference"
    assert narrative["evidence"][0]["article_id"] in {"article-001", "article-002"}


def test_articles_can_filter_by_perspective() -> None:
    response = client.get("/articles", params={"perspective": "center-right"})
    assert response.status_code == 200
    assert response.json()["total"] == 1
    assert response.json()["items"][0]["perspective"] == "center-right"
