<div align="center">

# api-prisma

**API REST somente leitura do PRISMA**: expõe políticos, partidos, candidaturas e a análise de sentimento da cobertura jornalística (parágrafos, matérias, temas e panoramas), sempre com proveniência e link para a fonte.

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-JPA-4169E1?logo=postgresql&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger_UI-85EA2D?logo=swagger&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-multi--stage-2496ED?logo=docker&logoColor=white)
![Deploy](https://img.shields.io/badge/deploy-ArgoCD-EF7B4D?logo=argo&logoColor=white)

[Organização PRISMA](https://github.com/prisma-projetos) · [Guia de endpoints](endpoints-api-prisma.md)

</div>

---

## Onde este módulo fica no PRISMA

```
scrapping-prisma ──▶ classifier-prisma ──▶ analyser-prisma ──▶ System DB ──▶ [ api-prisma ] ──▶ app-prisma
                                                                                ▲ você está aqui
```

A API **só lê**. Quem escreve no System DB são o `classifier-prisma` (classificações por parágrafo) e o `analyser-prisma` (resumos e panoramas). Por isso todos os endpoints são `GET`.

## Sumário

- [Stack](#stack)
- [Modelo de dados](#modelo-de-dados)
- [Endpoints](#endpoints)
- [Segurança](#segurança)
- [Como rodar](#como-rodar)
- [Profiles e configuração](#profiles-e-configuração)
- [Docker](#docker)
- [Testes](#testes)
- [CI/CD](#cicd)
- [Repositórios do PRISMA](#repositórios-do-prisma)

## Stack

| Tecnologia | Versão |
|---|---|
| Java | **25** (Eclipse Temurin) |
| Spring Boot | **4.1.0**: WebMVC, Data JPA, Security, Validation e WebClient |
| springdoc-openapi | 3.1.0 (Swagger UI) |
| Maven Wrapper | 3.3.4 (Maven 3.9.16) |
| PostgreSQL | Driver JDBC |
| Lombok / DevTools | Opcionais |

> Confira `java -version` antes de rodar: o projeto exige o JDK 25.

<details>
<summary><b>Estrutura do código</b></summary>

```
src/main/java/br/com/prisma/api_prisma/
├── ApiPrismaApplication.java
├── config/OpenApiConfig.java          # Swagger + esquema de API Key
├── controller/
│   ├── PoliticoController.java        # /api/politicos
│   ├── PartidoController.java         # /api/partidos
│   ├── AnaliseController.java         # sentimento, por-midia, mencoes (parágrafo)
│   └── AnaliseMateriaController.java  # materias, por-tema, panorama (analyser)
├── security/                          # ApiKeyFilter, SecurityConfig, SecurityConstants
├── exception/                         # NotFoundException + GlobalExceptionHandler (JSON padronizado)
├── model/                             # entidades JPA espelhando o schema real
├── repository/                        # Spring Data JPA
├── dto/                               # records de request/response + MinioUrls
└── service/                           # Alvo, CatalogoQueryService, AnaliseQueryService, AnaliseMateriaQueryService
```

</details>

## Modelo de dados

Os dados são **ancorados no TSE**: um político é identificado pelo `nr_titulo_eleitoral_candidato` e um partido pelo `nr_partido`. Cargo, UF e filiação ficam na **candidatura**, porque mudam a cada eleição.

| Tabela | Unidade | Quem escreve | O que traz |
|---|---|---|---|
| `tabela_classificacao` | Parágrafo × entidade | `classifier-prisma` | Sentimento, score de confiança, parágrafo |
| `analyser_item` | Matéria × ator | `analyser-prisma` | Resumo, temas, rótulo |
| `analyser_panorama` | Ator × mês × eixo | `analyser-prisma` | Texto consolidado, temas, fontes |
| `tabela_politico` · `tabela_partido` · `tabela_candidatura` · `tabela_eleicao` · `tabela_midia` · `entidade_matching` | Catálogo | Carga do TSE | Cadastro e aliases |

Pontos de atenção:

- **Cada análise aponta para um político _ou_ um partido**, nunca os dois. Por isso toda consulta de análise exige **exatamente um** entre `tituloEleitoral` e `nrPartido`.
- **`eixo` ≠ `tema`**: eixo é a polaridade (`positivo`, `neutro` ou `negativo`), e tema é a pauta, em texto livre atribuído pelo modelo.
- **Panorama:** a API retorna só a **maior `versao`** de cada `(entidade, janela, eixo)`.
- **Não existe título de matéria** persistido. Use `linkNoticia`.
- `id_artigo_scrape` aponta para o banco do scraper, que é externo e não tem FK.
- Fotos de candidatos e logos de partidos são URLs montadas para o **MinIO** (`MinioUrls`).

## Endpoints

Todos são `GET`, exigem `X-API-KEY` e recebem filtros por **query string**. O guia completo, com exemplos de resposta, está em [`endpoints-api-prisma.md`](endpoints-api-prisma.md).

| Rota | Descrição | Principais parâmetros |
|---|---|---|
| `/api/politicos` | Lista políticos por candidatura (paginado) | `q`, `nrPartido`, `anoEleicao`, `cargo`, `uf`, `pagina`, `tamanho` |
| `/api/politicos/detalhe` | Político e histórico de candidaturas | `tituloEleitoral` |
| `/api/partidos` | Lista partidos | `q` |
| `/api/partidos/detalhe` | Detalhe do partido | `nrPartido` |
| `/api/analise/sentimento` | Sentimento agregado, com método e `pipeline_version` | `tituloEleitoral` \| `nrPartido` |
| `/api/analise/por-midia` | Sentimento por veículo (mostra concentração de fontes) | idem |
| `/api/analise/mencoes` | Parágrafos analisados com link da notícia (paginado) | idem + `sentimento`, `de`, `ate` |
| `/api/analise/materias` | Matérias com resumo, temas, rótulo e modelo | idem + `tema`, `pagina`, `tamanho` |
| `/api/analise/por-tema` | Concentração de pauta por tema | idem |
| `/api/analise/panorama` | Texto consolidado do mês, por eixo, com fontes resolvidas | idem + `janela` (`YYYY-MM`) |

```bash
# sentimento agregado sobre o PT
curl -G -H "X-API-KEY: 123456" http://localhost:8080/api/analise/sentimento \
     --data-urlencode "nrPartido=13"

# parágrafos negativos sobre um político a partir de uma data
curl -G -H "X-API-KEY: 123456" http://localhost:8080/api/analise/mencoes \
     --data-urlencode "tituloEleitoral=122418060191" \
     --data-urlencode "sentimento=negativo" \
     --data-urlencode "de=2026-01-01"
```

<details>
<summary><b>O que a API faz questão de não esconder</b></summary>

- **Proveniência**: todo agregado sai com o `metodo` e a `pipeline_version` que o geraram, e todo resumo ou panorama sai com o `modelo` que o escreveu.
- **Parágrafos sem veículo**: cerca de 2/3 das classificações não têm `id_midia`. Elas aparecem em `paragrafosSemVeiculo` em vez de sumirem num `JOIN`.
- **Concentração**: `/por-midia` e `/por-tema` mostram quando um veículo ou tema domina o volume.
- **Rastreabilidade**: `/mencoes` traz o parágrafo exato e o link, e o panorama traz as matérias que sustentam cada tema. Um id sem matéria correspondente continua visível.
- **Leitura dos números**: o classificador atual é um léxico (cerca de 86% `neutro`, com `score=0`). Trate positivo e negativo como sinal fraco.

</details>

## Segurança

- Header **`X-API-KEY`** obrigatório em `/api/**`. Sem ele, a resposta é `401`. O Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) é público e fica **desligado em `prod`**.
- **A chave é validada no boot**, e a aplicação não sobe se ela estiver ausente ou vazia. Fora do profile `local`, também não sobe com menos de 16 caracteres.
- Erros não expõem detalhes internos: `include-stacktrace=never`, `include-message=never` e header `Server` vazio.

```bash
# gerar uma chave forte
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

## Como rodar

```bash
git clone git@github.com:prisma-projetos/api-prisma.git
cd api-prisma

docker compose -f docker/docker-compose.yml up -d    # Postgres local (prisma / postgres / postgres-prisma)
./mvnw spring-boot:run                               # profile "local" (Windows: mvnw.cmd)

curl -H "X-API-KEY: 123456" http://localhost:8080/api/partidos
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

<details>
<summary><b>Apontando para o banco de desenvolvimento (profile <code>dev</code>)</b></summary>

```bash
cp .env.example .env.local        # preencha SPRING_PROFILES_ACTIVE=dev, API_SECURITY_KEY, DB_URL, DB_USER, DB_PASS
set -a; source .env.local; set +a
./mvnw spring-boot:run
```

</details>

## Profiles e configuração

| Profile | Banco | `ddl-auto` | Swagger | Chave |
|---|---|---|---|---|
| `local` (padrão) | Docker Compose em `localhost:5432` | `update` só com banco descartável | Sim | `123456` fixa |
| `dev` | `DB_URL` / `DB_USER` / `DB_PASS` | `validate` | Sim | `API_SECURITY_KEY` |
| `prod` | Variáveis de ambiente | `validate` | Não | `API_SECURITY_KEY` |

O profile é escolhido com `SPRING_PROFILES_ACTIVE`, que tem precedência sobre o `application.properties`.

> Em `dev`/`prod` (`validate`), a aplicação **não sobe** se as tabelas do analyser ainda não existirem. Aplique antes `analyser-prisma/migrations/001_analyser.sql`.

## Docker

Build multi-stage: `eclipse-temurin:25-jdk` (com `dependency:go-offline` para aproveitar o cache) gera o artefato, e `eclipse-temurin:25-jre` executa. `TZ=America/Fortaleza`, porta `8080`.

```bash
docker build -f docker/Dockerfile -t api-prisma .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/prisma \
  -e DB_USER=postgres -e DB_PASS=postgres-prisma \
  -e API_SECURITY_KEY="$(python -c 'import secrets;print(secrets.token_urlsafe(32))')" \
  api-prisma
```

## Testes

```bash
./mvnw test
```

Cobre os controllers (`Politico`, `Analise`, `AnaliseMateria`), o `ApiKeyFilter`, o `AnaliseItemRepository` e os serviços de consulta (`CatalogoQueryService`, `AnaliseMateriaQueryService`).

## CI/CD

`.gitea/workflows/ci-cd.yaml`: em push para `main`/`develop`, faz build da imagem → atualiza os manifestos → sincroniza no **ArgoCD**. Em produção, `SPRING_PROFILES_ACTIVE=prod` e as credenciais vêm de *secrets* do cluster.

## Repositórios do PRISMA

| Repositório | Papel |
|---|---|
| [`.github`](https://github.com/prisma-projetos) | Página da organização |
| [`scrapping-prisma`](https://github.com/prisma-projetos/scrapping-prisma) | Coleta de notícias |
| [`classifier-prisma`](https://github.com/prisma-projetos/classifier-prisma) | Identificação de entidades e classificação do teor |
| [`analyser-prisma`](https://github.com/prisma-projetos/analyser-prisma) | Resumos e panoramas com LLM |
| **`api-prisma`** | API REST somente leitura *(este)* |
| [`app-prisma`](https://github.com/prisma-projetos/app-prisma) | Interface web |
| [`experiments-prisma`](https://github.com/prisma-projetos/experiments-prisma) | Documentação, ADRs e experimentos do artigo |
