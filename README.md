# api-prisma

API REST desenvolvida em **Spring Boot**, com autenticação via **API Key**, persistência em **PostgreSQL** (via Spring Data JPA) e documentação automática via **Swagger / OpenAPI**.

É a **camada de consulta** do Prisma — o sistema que coleta notícias sobre políticos e partidos brasileiros e analisa o que está sendo dito sobre cada um, de forma rastreável até a matéria de origem.

A API é **somente leitura**: quem escreve neste banco são o `classifier-prisma`, que grava as classificações por parágrafo, e o `analyser-prisma`, que grava os resumos e os panoramas consolidados. Antes disso, a coleta roda no `scrapper-prisma`, que tem banco próprio e é o único a acessá-lo — a fronteira entre os componentes é o RabbitMQ, não o banco. Por isso todos os endpoints são `GET`.

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
│   ├── PoliticoController.java      # políticos e histórico de candidaturas
│   ├── PartidoController.java       # partidos
│   ├── AnaliseController.java       # sentimento agregado e menções (parágrafo)
│   └── AnaliseMateriaController.java # eixo, título e resumo (matéria)
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
│   ├── AnaliseItem.java             # analyser_item (matéria + resumo/temas)
│   ├── Panorama.java                # analyser_panorama (texto consolidado do mês)
│   └── EntidadeMatching.java        # entidade_matching (nomes e aliases)
├── repository/                      # repositórios Spring Data JPA
├── dto/                             # contratos de request/response (records)
└── service/
    ├── Alvo.java                    # político OU partido, e a tradução p/ os sentinelas
    ├── AnaliseQueryService.java     # consultas no nível do parágrafo
    ├── AnaliseMateriaQueryService.java # matéria, tema e panorama (analyser)
    └── CatalogoQueryService.java    # políticos, partidos e candidaturas
```

## 🗄️ O modelo de dados

O banco é ancorado em **dados do TSE**. Um político é identificado pelo `nr_titulo_eleitoral_candidato`; um partido, pelo `nr_partido`. Cargo, UF e filiação **não** pertencem à pessoa — vivem na candidatura, porque mudam de uma eleição para outra.

A análise vive em `tabela_classificacao`, e três características dela mudam como a API deve ser lida:

- **A unidade analisada é o parágrafo**, não a notícia. Uma matéria longa gera várias linhas e pesa mais no agregado que uma nota curta.
- **Cada linha aponta para um político OU um partido**, nunca os dois. Por isso toda consulta de análise exige exatamente um dos dois identificadores.
- **A saída é sentimento** (`positivo` / `neutro` / `negativo`), produzido hoje pelo método `lexicon-baseline-v1`. Não é classificação de espectro político, e a API não afirma que seja.

`id_artigo_scrape` referencia o artigo no banco do scraper, que é **externo** a este banco — não há FK nem entidade JPA para ele. Para chegar à matéria, use `linkNoticia`.

### Dois níveis de análise

A API lê **duas** tabelas de análise, em granularidades diferentes e escritas por componentes diferentes. Elas convivem e nenhuma depende da outra — não há FK entre as duas. Para cruzar, o caminho é `(id_artigo_scrape, entidade)`.

| Tabela | Unidade | Quem escreve | O que traz |
|---|---|---|---|
| `tabela_classificacao` | parágrafo | `classifier-prisma` | sentimento, score de confiança |
| `analyser_item` | matéria × ator | `analyser-prisma` | resumo, temas, rótulo da matéria |
| `analyser_panorama` | ator × mês × eixo | `analyser-prisma` | texto consolidado, temas contados, fontes |

As três tabelas vivem no mesmo **System DB** compartilhado (ver ADR 0002 do `classifier-prisma`): o classifier e o analyser escrevem, a API só lê. As migrations do analyser estão em `analyser-prisma/migrations/001_analyser.sql`.

> ⚠️ **`eixo` e `tema` não são a mesma coisa — e a confusão entre os dois é fácil.** No vocabulário do analyser, **`eixo` é polaridade de sentimento**: o banco restringe a coluna a `positivo`, `negativo` e `neutro`. O recorte de pauta se chama **`tema`**, é texto livre atribuído por modelo, e uma matéria pode ter vários. Concentração de pauta se mede por tema (`/analise/por-tema`); a distribuição por polaridade no nível da matéria é o campo `rotulo`.

> 📌 **Não existe título de matéria.** O analyser recebe um título ao gerar o resumo, mas não o persiste — não está na lista de colunas do `INSERT` em `gravar_resumo`. Nenhuma resposta desta API promete `titulo`: um campo eternamente nulo seria pior que a ausência dele. Para exibir a matéria, o caminho é `linkNoticia`.

> 📌 **Versão do panorama.** `analyser_panorama` é append-only: cada recálculo insere uma versão nova e nenhuma é sobrescrita. A API expõe apenas a **maior versão** de cada `(entidade, janela, eixo)`. A coluna `publicado_em` existe na migration mas o analyser nunca a escreve — filtrar por ela devolveria vazio para sempre, então a seleção é por `max(versao)`, o mesmo critério que o próprio analyser usa.

## 📡 Endpoints

Todos exigem `X-API-KEY` e são somente leitura.

O identificador da entidade vai **no corpo da requisição**, não no path. As consultas de análise aceitam `tituloEleitoral` (político) **ou** `nrPartido` (partido) — exatamente um dos dois; os dois juntos, ou nenhum, é `400`.

| Método | Rota | O que responde |
|---|---|---|
| `GET` | `/api/politicos` | Lista políticos por candidatura (`?q=`, `?nrPartido=`, `?anoEleicao=`, `?cargo=`, `?pagina=`, `?tamanho=`) |
| `GET` | `/api/politicos/detalhe` | Político e histórico de candidaturas |
| `GET` | `/api/partidos` | Lista partidos (`?q=`) |
| `GET` | `/api/partidos/detalhe` | Detalhe de um partido |
| `GET` | `/api/analise/sentimento` | Sentimento agregado sobre a entidade |
| `GET` | `/api/analise/por-midia` | Sentimento aberto por veículo |
| `GET` | `/api/analise/mencoes` | Parágrafos analisados, com link da notícia |
| `GET` | `/api/analise/materias` | Matérias analisadas: resumo, temas e rótulo |
| `GET` | `/api/analise/por-tema` | Matérias abertas por tema (concentração de pauta) |
| `GET` | `/api/analise/panorama` | Texto consolidado do mês, por eixo, com fontes resolvidas |

Os três últimos endpoints de análise leem as tabelas do `analyser-prisma`. Elas precisam existir no banco: em `dev`/`prod` (`ddl-auto=validate`) a aplicação **não sobe** enquanto o analyser não tiver aplicado `migrations/001_analyser.sql`.

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
- **Concentração de pauta** — `/analise/por-tema` mostra o peso de cada tema. Se quase toda a cobertura cai num tema só, o agregado descreve aquele recorte, não a cobertura em geral. Matérias sem tema atribuído aparecem em grupo próprio, pelo mesmo motivo dos parágrafos sem veículo. Uma matéria com vários temas conta em cada um, então a soma dos totais passa do total de matérias — o percentual é sempre sobre as matérias.
- **Resumo não é fato** — `/analise/materias` devolve `resumo` sempre acompanhado do `modelo` que o escreveu, e `/analise/panorama` faz o mesmo com o texto consolidado. Resumo é saída de modelo, sem exceção: um resumo circulando sem dizer quem o escreveu quebraria a regra de proveniência que o resto da API cumpre.
- **Fonte de cada afirmação** — no panorama, cada tema vem com as matérias que o sustentam, já resolvidas para veículo e link. Um id citado pelo analyser que não tenha matéria correspondente **continua aparecendo**, só com o id: escondê-lo faria o texto citar uma matéria que a tela não mostra.

> ⚠️ **Sobre ler os números.** O classificador atual é um baseline léxico: ~86% das classificações são `neutro`, e nelas o `score_confianca` é `0.0`. Um agregado dominado por neutro diz mais sobre o método do que sobre a cobertura. Trate `positivo`/`negativo` como sinal fraco até existir um classificador melhor.

## 🔐 Segurança (API Key)

Todas as rotas exigem autenticação via header **`X-API-KEY`**, exceto os endpoints do Swagger (`/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`), que são públicos.

O valor esperado da chave vem da propriedade `api.security.key`, injetada em `ApiKeyFilter` e `SecurityConstants`.

O `ApiKeyFilter` valida a chave **no boot**, não na primeira requisição — uma chave ausente subiria a aplicação "funcionando", com a API inteira efetivamente aberta. Duas regras derrubam a inicialização:

- **Chave ausente ou vazia** → falha em qualquer profile.
- **Menos de 16 caracteres** → falha fora do profile `local`. No `local` só emite `WARN`, porque ali a chave é fixa e curta de propósito, contra um banco descartável.

Ou seja: `-e API_SECURITY_KEY=minha-chave` (11 caracteres) com profile `dev` **não sobe**. Gere uma chave aleatória longa para `dev` e `prod`.

Exemplo de chamada autenticada:
```bash
curl -H "X-API-KEY: 123456" http://localhost:8080/api/partidos
```

## ⚙️ Profiles e `application.properties`

O projeto usa profiles do Spring para separar configuração por ambiente.

### `application.properties` (raiz)
```properties
spring.application.name=api-prisma
server.port=8080
spring.profiles.active=local
```
- Define a porta padrão (`8080`).
- Define `local` como profile padrão. Para trocar, passe a env **`SPRING_PROFILES_ACTIVE`** (`dev` ou `prod`): variáveis de ambiente têm precedência sobre o `application.properties`, então o deploy sobrescreve isto sem nenhum placeholder no arquivo.

> 📌 **Antes esta linha era `spring.profiles.active=${API_SECURITY_KEY:local}`**, ou seja, o nome do profile vinha da variável da *chave da API*. Isso tornava as duas coisas impossíveis de satisfazer ao mesmo tempo: `API_SECURITY_KEY=dev` ativava o profile certo mas deixava a chave com 3 caracteres (rejeitada no boot, ver [Segurança](#-segurança-api-key)), e uma chave real ativava um profile inexistente, sem datasource. Corrigido.

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
4. Teste um endpoint protegido:
   ```bash
   curl -H "X-API-KEY: 123456" http://localhost:8080/api/partidos
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
  -e API_SECURITY_KEY=troque-por-uma-chave-aleatoria-longa \
  api-prisma
```

> Note que aqui o `docker-compose.yml` atual só sobe o **banco**; não há um serviço para a aplicação em si no compose. Se quiser subir tudo junto (app + banco) num único `docker compose up`, seria necessário adicionar um serviço `app` a esse `docker-compose.yml` usando esse `Dockerfile`.

## 📌 Resumo rápido

| Ambiente | Profile | Banco | Swagger |
|---|---|---|---|
| Local (dev na máquina) | `local` | Docker Compose (`localhost:5432`) | habilitado |
| Desenvolvimento (deploy) | `dev` | via env vars (`DB_URL`, `DB_USER`, `DB_PASS`) | habilitado |
| Produção | `prod` | via env vars | **desabilitado** |
