# api-prisma

API REST desenvolvida em **Spring Boot**, com autenticação via **API Key**, persistência em **PostgreSQL** (via Spring Data JPA) e documentação automática via **Swagger / OpenAPI**.

É a **camada de consulta** do Prisma — o sistema que coleta notícias sobre políticos e partidos brasileiros e analisa o que está sendo dito sobre cada um, de forma rastreável até a matéria de origem.

A API é **somente leitura**: quem escreve no banco é o pipeline (scraper, matching de entidades e classificação), que roda como componentes independentes. Por isso todos os endpoints são `GET`.

## 🚀 Tecnologias e versões



| Tecnologia | Versão |
|---|---|
| Java | **25** |
| Spring Boot | **4.1.0** |
| Maven Wrapper | 3.3.4 (Maven 3.9.16) |
| Spring Security | via `spring-boot-starter-security` |
| Spring Data JPA | via `spring-boot-starter-data-jpa` |
| Spring WebMVC | via `spring-boot-starter-webmvc` |
| Spring WebClient | via `spring-boot-starter-webclient` |
| springdoc-openapi (Swagger UI) | 3.1.0 |
| PostgreSQL Driver | (runtime) |
| Lombok | (opcional, geração de boilerplate) |
| Spring Boot DevTools | (runtime, hot reload) |

> ⚠️ Java 25 e Spring Boot 4.1.0 são versões bastante recentes — garanta que sua IDE e seu JDK local estejam realmente na versão 25 antes de rodar o projeto (`java -version`).

## 📁 Estrutura do projeto

```
src/main/java/br/com/prisma/api_prisma/
├── ApiPrismaApplication.java        # classe main (@SpringBootApplication)
├── config/
│   └── OpenApiConfig.java           # configuração do Swagger (esquema de API Key)
├── controller/
│   ├── TestController.java          # endpoint de teste (/api/test)
│   ├── PoliticoController.java      # políticos e histórico de candidaturas
│   ├── PartidoController.java       # partidos
│   └── AnaliseController.java       # sentimento agregado e menções
├── security/
│   ├── SecurityConfig.java          # filtro de segurança (Spring Security)
│   ├── ApiKeyFilter.java            # valida o header X-API-KEY
│   └── SecurityConstants.java       # carrega a chave configurada
├── exception/
│   ├── NotFoundException.java       # recurso inexistente → 404
│   └── GlobalExceptionHandler.java  # respostas de erro em JSON padronizado
├── model/                           # entidades JPA, espelhando o schema real
│   ├── Politico.java                # tabela_politico (PK: título eleitoral)
│   ├── Partido.java                 # tabela_partido (PK: número de legenda)
│   ├── Candidatura.java             # tabela_candidatura (cargo, UF, partido, resultado)
│   ├── Eleicao.java                 # tabela_eleicao
│   ├── Midia.java                   # tabela_midia (veículo de imprensa)
│   ├── Classificacao.java           # tabela_classificacao (parágrafo + sentimento)
│   └── EntidadeMatching.java        # entidade_matching (nomes e aliases)
├── repository/                      # repositórios Spring Data JPA
├── dto/                             # contratos de request/response (records)
└── service/
    └── AnaliseQueryService.java     # consultas de leitura
```

## 🗄️ O modelo de dados

O banco é ancorado em **dados do TSE**. Um político é identificado pelo `nr_titulo_eleitoral_candidato`; um partido, pelo `nr_partido`. Cargo, UF e filiação **não** pertencem à pessoa — vivem na candidatura, porque mudam de uma eleição para outra.

A análise vive em `tabela_classificacao`, e três características dela mudam como a API deve ser lida:

- **A unidade analisada é o parágrafo**, não a notícia. Uma matéria longa gera várias linhas e pesa mais no agregado que uma nota curta.
- **Cada linha aponta para um político OU um partido**, nunca os dois. Por isso toda consulta de análise exige exatamente um dos dois identificadores.
- **A saída é sentimento** (`positivo` / `neutro` / `negativo`), produzido hoje pelo método `lexicon-baseline-v1`. Não é classificação de espectro político, e a API não afirma que seja.

`id_artigo_scrape` referencia o artigo no banco do scraper, que é **externo** a este banco — não há FK nem entidade JPA para ele. Para chegar à matéria, use `linkNoticia`.

## 📡 Endpoints

Todos exigem `X-API-KEY` e são somente leitura.

O identificador da entidade vai **no corpo da requisição**, não no path. As consultas de análise aceitam `tituloEleitoral` (político) **ou** `nrPartido` (partido) — exatamente um dos dois; os dois juntos, ou nenhum, é `400`.

| Método | Rota | O que responde |
|---|---|---|
| `GET` | `/api/politicos` | Lista políticos (`?q=`, `?pagina=`, `?tamanho=`) |
| `GET` | `/api/politicos/detalhe` | Político e histórico de candidaturas |
| `GET` | `/api/partidos` | Lista partidos (`?q=`) |
| `GET` | `/api/partidos/detalhe` | Detalhe de um partido |
| `GET` | `/api/analise/sentimento` | Sentimento agregado sobre a entidade |
| `GET` | `/api/analise/por-midia` | Sentimento aberto por veículo |
| `GET` | `/api/analise/mencoes` | Parágrafos analisados, com link da notícia |
| `GET` | `/api/test` | Verificação de disponibilidade e da chave |

```bash
# sentimento agregado sobre o PT
curl -X GET -H "X-API-KEY: 123456" -H "Content-Type: application/json" \
     -d '{"nrPartido": 13}' \
     http://localhost:8080/api/analise/sentimento

# paragrafos negativos sobre um politico, a partir de uma data
curl -X GET -H "X-API-KEY: 123456" -H "Content-Type: application/json" \
     -d '{"tituloEleitoral": "122418060191", "sentimento": "negativo", "de": "2026-01-01"}' \
     http://localhost:8080/api/analise/mencoes
```

> ⚠️ O Swagger UI **não envia corpo em requisição GET** — é limitação do `fetch` do navegador, não do springdoc. O `requestBody` aparece documentado na tela, mas o botão "Try it out" dispara sem ele. Para testar interativamente, use `curl` ou Postman. Trocar `@GetMapping` por `@PostMapping` resolveria, mantendo rotas e corpo idênticos.

### O que a API não esconde

- **Proveniência** — `/analise/sentimento` sempre devolve o `metodo` e a `pipeline_version` que produziram os números. O agregado não circula sem o modelo que o gerou.
- **Parágrafos sem veículo** — hoje **cerca de dois terços** das classificações não têm `id_midia`. Um `join` interno as descartaria em silêncio e a cobertura pareceria menor do que é; elas voltam em `paragrafosSemVeiculo` e num grupo próprio no recorte por veículo.
- **Concentração de fontes** — `/analise/por-midia` mostra o peso de cada veículo. Se quase todo o volume de uma entidade vem de um só veículo, o agregado descreve a linha editorial dele, não a cobertura em geral.
- **Rastreabilidade** — `/analise/mencoes` devolve o parágrafo exato que originou cada classificação, com o link da matéria.

> ⚠️ **Sobre ler os números.** O classificador atual é um baseline léxico: ~86% das classificações são `neutro`, e nelas o `score_confianca` é `0.0`. Um agregado dominado por neutro diz mais sobre o método do que sobre a cobertura. Trate `positivo`/`negativo` como sinal fraco até existir um classificador melhor.

## 🔐 Segurança (API Key)

Todas as rotas exigem autenticação via header **`X-API-KEY`**, exceto os endpoints do Swagger (`/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`), que são públicos.

O valor esperado da chave vem da propriedade `api.security.key`, injetada em `ApiKeyFilter` e `SecurityConstants`.

Exemplo de chamada autenticada:
```bash
curl -H "X-API-KEY: 123456" http://localhost:8080/api/test
```

## ⚙️ Profiles e `application.properties`

O projeto usa profiles do Spring para separar configuração por ambiente.

### `application.properties` (raiz)
```properties
spring.application.name=api-prisma
server.port=8080
spring.profiles.active=${API_SECURITY_KEY:local}
```
- Define a porta padrão (`8080`).
- Define qual profile será ativado, lendo da variável de ambiente `API_SECURITY_KEY`, com fallback para `local`.

> ⚠️ **Atenção, possível inconsistência:** essa linha usa a variável `API_SECURITY_KEY` para decidir o **profile ativo** (`local`, `dev` ou `prod`), quando o nome sugere que deveria conter a *chave da API*, não o *nome do profile*. O padrão mais comum seria usar a variável `SPRING_PROFILES_ACTIVE` para isso. Vale revisar se foi intencional.

### `application-local.properties` (usado por padrão)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/prisma
spring.datasource.username=postgres
spring.datasource.password=postgres-prisma

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

api.security.key=123456
```
- Aponta para o banco local (o mesmo criado pelo `docker-compose.yml`).
- `ddl-auto=update` → Hibernate cria/atualiza as tabelas automaticamente (bom para desenvolvimento).
- `show-sql=true` e `format_sql=true` → loga o SQL executado, formatado, no console.
- Chave de API fixa: `123456`.

> ⚠️ O profile `local` usa `ddl-auto=update`, que deixa o Hibernate **criar e alterar tabelas**. Contra o banco real isso é perigoso: use `local` só com um Postgres descartável. Para apontar para o banco de desenvolvimento, use o profile `dev`, que é `validate` — ele só confere se as entidades batem com o schema e não altera nada.

### `application-dev.properties`
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

api.security.key=${API_SECURITY_KEY}
```
- Credenciais do banco vindas de variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASS`).
- `ddl-auto=validate` → apenas valida se o schema bate com as entidades (não altera o banco).
- Chave de API também vinda de variável de ambiente.

### `application-prod.properties`
- Igual ao `dev`, porém desabilita o Swagger:
```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

## 🐳 Banco de dados local com Docker Compose

O arquivo `docker/docker-compose.yml` sobe um PostgreSQL pronto para o profile `local`:

```yaml
services:
  db:
    image: postgres:15-alpine
    container_name: prisma-postgres
    restart: always
    environment:
      - POSTGRES_DB=prisma
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres-prisma
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Essas credenciais batem exatamente com o `application-local.properties`, então basta subir o container e rodar a aplicação.

### Subindo o banco

```bash
cd docker
docker compose up -d
```

Isso cria/inicia o container `prisma-postgres`, expõe a porta `5432` e persiste os dados no volume `postgres_data`.

Para derrubar:
```bash
docker compose down
```

Para derrubar **e apagar os dados** (reset completo do banco):
```bash
docker compose down -v
```

## ▶️ Rodando o projeto localmente

1. **Suba o banco** (passo acima).
2. **Rode a aplicação** com o Maven Wrapper (usa o profile `local` por padrão, já que `API_SECURITY_KEY` não estará setada):
   ```bash
   ./mvnw spring-boot:run
   ```
   (No Windows: `mvnw.cmd spring-boot:run`)
3. A API sobe em `http://localhost:8080`.
4. Teste o endpoint protegido:
   ```bash
   curl -H "X-API-KEY: 123456" http://localhost:8080/api/test
   ```

### Documentação Swagger

Com a aplicação rodando (profile `local` ou `dev`), acesse:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

(No profile `prod` esses endpoints ficam desabilitados.)

## 🐋 Build e execução via Docker (imagem da aplicação)

O `docker/Dockerfile` faz um build multi-stage:

1. **Stage build**: usa `eclipse-temurin:25-jdk`, copia o código e roda `./mvnw clean package -DskipTests`.
2. **Stage runtime**: usa `eclipse-temurin:25-jre` (imagem enxuta, só com JRE), copia o `.jar` gerado e expõe a porta `8080`. Timezone fixado em `America/Fortaleza`.

Para buildar e rodar a imagem da aplicação (a partir da raiz do projeto):
```bash
docker build -f docker/Dockerfile -t api-prisma .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/prisma \
  -e DB_USER=postgres \
  -e DB_PASS=postgres-prisma \
  -e API_SECURITY_KEY=minha-chave \
  api-prisma
```

> Note que aqui o `docker-compose.yml` atual só sobe o **banco**; não há um serviço para a aplicação em si no compose. Se quiser subir tudo junto (app + banco) num único `docker compose up`, seria necessário adicionar um serviço `app` a esse `docker-compose.yml` usando esse `Dockerfile`.

## 📌 Resumo rápido

| Ambiente | Profile | Banco | Swagger |
|---|---|---|---|
| Local (dev na máquina) | `local` | Docker Compose (`localhost:5432`) | habilitado |
| Desenvolvimento (deploy) | `dev` | via env vars (`DB_URL`, `DB_USER`, `DB_PASS`) | habilitado |
| Produção | `prod` | via env vars | **desabilitado** |
