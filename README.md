# API de Análise de Viés Político

API somente leitura para consultar conteúdos classificados e sínteses agregadas por entidade e perspectiva política.

## Executar

```powershell
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Documentação interativa: http://127.0.0.1:8000/docs

A API é pública e somente leitura. Em produção, restrinja o acesso com autenticação no proxy reverso, rede privada ou outro mecanismo adequado ao ambiente.

## HTTPS

Em desenvolvimento, gere um certificado local e uma chave privada fora do controle de versão. Com OpenSSL instalado:

```powershell
New-Item -ItemType Directory certs -Force
openssl req -x509 -newkey rsa:2048 -keyout certs/localhost.key -out certs/localhost.crt -days 365 -nodes -subj "/CN=localhost"
.\run_https.ps1
```

A API ficará disponível em https://127.0.0.1:8443/docs. O navegador exibirá um alerta para certificado autoassinado, esperado apenas no ambiente local.

Em produção, use um certificado emitido por uma autoridade confiável e termine TLS em um proxy reverso (Nginx, Caddy ou serviço de nuvem). A criptografia do banco deve ser configurada separadamente no PostgreSQL, com chaves gerenciadas por um gerenciador de segredos.

## Endpoints

- `GET /health`: verifica disponibilidade.
- `GET /articles?entity=partido-alpha&perspective=center-right`: lista artigos e metadados de classificação.
- `GET /analyses/partido-alpha`: retorna sínteses por perspectiva, separando fatos, opiniões e inferências e mantendo as evidências.

O repositório atual é em memória e os resultados são demonstrativos. Em produção, substitua `InMemoryRepository` por um adaptador de banco sem alterar os contratos HTTP.
