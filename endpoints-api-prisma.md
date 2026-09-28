# API Prisma — Guia de Endpoints

Guia para consumo pelo frontend: políticos, partidos e análise de sentimento/matérias.

## Autenticação

Toda rota sob `/api/**` exige o header `X-API-KEY` com a chave configurada no ambiente. Sem o header (ou com valor errado), a API responde `401 Unauthorized`. As únicas rotas públicas são a documentação (`/swagger-ui.html` e `/v3/api-docs`).

```bash
curl -H "X-API-KEY: SUA_CHAVE_AQUI" http://localhost:8080/api/partidos
```

## URL base

Em desenvolvimento local: `http://localhost:8080`. Em homologação/produção, troque pelo host do ambiente.

## Formato de erro

Toda resposta de erro (400, 401, 404, 500) segue o mesmo formato:

```json
{
  "timestamp": "2026-09-24T10:15:00-03:00",
  "status": 404,
  "error": "Not Found",
  "message": "Politico com titulo eleitoral '000000000000' nao encontrado."
}
```

## Paginação

Endpoints que paginam (marcados abaixo) devolvem o objeto `Page` do Spring:

```json
{
  "content": ["... itens da pagina ..."],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 20,
  "size": 20,
  "totalElements": 187,
  "totalPages": 10
}
```

`content` é a lista de itens da página atual; o resto é metadado. Peça a próxima página incrementando `pagina` (base 0) até `last` vir `true`. `tamanho` máximo aceito é 100, mesmo se um valor maior for enviado.

---

## Políticos

Consulta de candidatos e do histórico de candidaturas. A base atual carrega apenas o pleito de 2026.

### `GET /api/politicos` — Lista políticos (por candidatura) · paginado

Busca por nome ou nome social, paginada. **Um item por candidatura**, não por pessoa: sem `anoEleicao`, um político com candidaturas em mais de uma eleição aparece repetido, uma vez por eleição. Já vem pronto para montar o mostruário de cards clicáveis da página do partido: `tituloEleitoral`, `nome`, `cargo`, `urlFoto`, `nrPartido` e `siglaPartido`. O partido é o **da candidatura** (não a filiação atual do político); `nrPartido` e `siglaPartido` vêm `null` se a candidatura não tiver partido.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `q` | string | Não | Trecho do nome ou nome social (case-insensitive) |
| `nrPartido` | integer | Não | Filtra por número de legenda do partido, ex. 22 |
| `anoEleicao` | integer | Não | Filtra por ano da eleição, ex. 2026 |
| `cargo` | string | Não | Filtra pelo cargo da candidatura (case-insensitive), ex. `PRESIDENTE`, `SENADOR`, `DEPUTADO FEDERAL` |
| `pagina` | integer | Não | Página, base 0. Padrão: 0 |
| `tamanho` | integer | Não | Itens por página, max 100. Padrão: 20 |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/politicos" \
  --data-urlencode "nrPartido=22" \
  --data-urlencode "anoEleicao=2026" \
  --data-urlencode "cargo=DEPUTADO FEDERAL" \
  --data-urlencode "pagina=0" \
  --data-urlencode "tamanho=20"
```

```json
{
  "content": [
    {
      "tituloEleitoral": "062567640809",
      "nome": "ABIMAEL DOS SANTOS PEREIRA LEAL",
      "cargo": "DEPUTADO FEDERAL",
      "urlFoto": "https://api-minio.ajvale.com.br/2026/fotosCandidatos/PE/FPE170002539230_div.jpg",
      "nrPartido": 22,
      "siglaPartido": "PL"
    },
    {
      "tituloEleitoral": "003013841872",
      "nome": "ABMAEL BORGES DA SILVEIRA",
      "cargo": "DEPUTADO FEDERAL",
      "urlFoto": "https://api-minio.ajvale.com.br/2026/fotosCandidatos/MT/FMT110002539002_div.jpg",
      "nrPartido": 22,
      "siglaPartido": "PL"
    }
  ],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 20,
  "size": 20,
  "totalElements": 1629,
  "totalPages": 82
}
```

> Uso típico no frontend: consumir esta lista para desenhar o mostruário de fotos da página do partido (`urlFoto` como `src` da imagem, `nome` embaixo, `cargo` para categorizar/filtrar os cards, `tituloEleitoral` guardado para o clique) e paginar conforme o usuário rola/pede mais.

### `GET /api/politicos/detalhe` — Detalha um político

Dados pessoais completos da pessoa mais o histórico de **todas** as suas candidaturas. Cargo, UF e partido vivem na candidatura, não na pessoa, porque podem mudar de uma eleição para outra. Use o `tituloEleitoral` obtido em `/api/politicos` para chamar esta rota quando o usuário clicar num card.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Sim | Título eleitoral do político |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/politicos/detalhe" \
  --data-urlencode "tituloEleitoral=062567640809"
```

```json
{
  "politico": {
    "tituloEleitoral": "062567640809",
    "nome": "ABIMAEL DOS SANTOS PEREIRA LEAL",
    "nomeSocial": null,
    "dataNascimento": "1978-04-12",
    "ufNascimento": "PE",
    "genero": "MASCULINO",
    "grauInstrucao": "ENSINO MEDIO COMPLETO",
    "corRaca": "PARDA",
    "ocupacao": "COMERCIANTE",
    "descricao": null
  },
  "candidaturas": [
    {
      "sequencial": 170002539230,
      "anoEleicao": 2026,
      "turno": 1,
      "cargo": "DEPUTADO ESTADUAL",
      "uf": "PE",
      "unidadeEleitoral": "PERNAMBUCO",
      "numeroUrna": 22123,
      "nomeUrna": "ABIMAEL LEAL",
      "nrPartido": 22,
      "siglaPartido": "PL",
      "situacaoCandidatura": "APTO",
      "resultado": null,
      "coligacao": "PARA MUDAR PERNAMBUCO",
      "federacao": null,
      "urlFoto": "https://api-minio.ajvale.com.br/2026/fotosCandidatos/PE/FPE170002539230_div.jpg"
    }
  ]
}
```

> Se o título eleitoral não existir na base, a resposta é `404 Not Found` no formato de erro padrão.

---

## Partidos

Consulta de partidos políticos.

### `GET /api/partidos` — Lista partidos

Busca por sigla ou nome. Não pagina: devolve a lista completa que bater com o filtro (poucas dezenas de partidos no total).

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `q` | string | Não | Trecho da sigla ou do nome (case-insensitive) |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/partidos" \
  --data-urlencode "q=PT"
```

```json
[
  {
    "numero": 13,
    "sigla": "PT",
    "nome": "PARTIDO DOS TRABALHADORES",
    "descricao": null,
    "urlLogo": "https://api-minio.ajvale.com.br/2026/fotosPartidos/PT.svg"
  }
]
```

### `GET /api/partidos/detalhe` — Detalha um partido

Busca um único partido pelo número de legenda.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `nrPartido` | integer | Sim | Número de legenda do partido, ex. 13 |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/partidos/detalhe" \
  --data-urlencode "nrPartido=13"
```

```json
{
  "numero": 13,
  "sigla": "PT",
  "nome": "PARTIDO DOS TRABALHADORES",
  "descricao": null,
  "urlLogo": "https://api-minio.ajvale.com.br/2026/fotosPartidos/PT.svg"
}
```

> `nrPartido` ausente ou partido inexistente respondem 400/404 no formato de erro padrão.

---

## Análise de sentimento

Sentimento agregado por parágrafo sobre um político ou um partido. Informe `tituloEleitoral` **ou** `nrPartido` na query string — nunca os dois na mesma chamada. Nenhum ou os dois juntos resultam em `400`.

### `GET /api/analise/sentimento` — Sentimento agregado sobre a entidade

A unidade contada é o parágrafo, não a notícia. A resposta sempre traz o método e a versão do pipeline (proveniência) que produziram os números.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Ver nota | Título eleitoral do político-alvo |
| `nrPartido` | integer | Ver nota | Número de legenda do partido-alvo |
| `de` | date (YYYY-MM-DD) | Não | Início do período de publicação |
| `ate` | date (YYYY-MM-DD) | Não | Fim do período de publicação |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/analise/sentimento" \
  --data-urlencode "nrPartido=13"
```

```json
{
  "tipoEntidade": "partido",
  "identificador": "13",
  "nome": "PARTIDO DOS TRABALHADORES",
  "periodoInicio": "2026-01-01",
  "periodoFim": "2026-09-24",
  "totalParagrafos": 4820,
  "porSentimento": [
    { "sentimento": "positivo", "total": 1120, "percentual": 23.24, "scoreMedio": 0.81 },
    { "sentimento": "neutro", "total": 2400, "percentual": 49.79, "scoreMedio": 0.74 },
    { "sentimento": "negativo", "total": 1300, "percentual": 26.97, "scoreMedio": 0.79 }
  ],
  "proveniencia": [
    { "metodo": "lexicon-baseline-v1", "pipelineVersion": "1.3.0", "total": 4820 }
  ]
}
```

### `GET /api/analise/por-midia` — Sentimento aberto por veículo

Mesmo agregado de sentimento, mas dividido por veículo de imprensa. Serve para auditar concentração de fonte: se quase todo o volume vem de um único veículo, o agregado descreve a linha editorial dele, não a cobertura em geral.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Ver nota | Título eleitoral do político-alvo |
| `nrPartido` | integer | Ver nota | Número de legenda do partido-alvo |
| `de` | date (YYYY-MM-DD) | Não | Início do período de publicação |
| `ate` | date (YYYY-MM-DD) | Não | Fim do período de publicação |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/analise/por-midia" \
  --data-urlencode "nrPartido=13"
```

```json
{
  "tipoEntidade": "partido",
  "identificador": "13",
  "nome": "PARTIDO DOS TRABALHADORES",
  "veiculosDistintos": 18,
  "totalParagrafos": 4820,
  "paragrafosSemVeiculo": 42,
  "midias": [
    {
      "idMidia": 7,
      "nomeMidia": "G1",
      "total": 950,
      "percentualDoTotal": 19.71,
      "positivo": 210,
      "neutro": 480,
      "negativo": 260
    }
  ]
}
```

### `GET /api/analise/mencoes` — Parágrafos analisados · paginado

Rastreabilidade: o trecho exato de texto que originou cada classificação, com o link da notícia. Aceita filtro por sentimento e por período.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Ver nota | Título eleitoral do político-alvo |
| `nrPartido` | integer | Ver nota | Número de legenda do partido-alvo |
| `sentimento` | string | Não | `positivo`, `neutro` ou `negativo` |
| `de` | date (YYYY-MM-DD) | Não | Início do período de publicação |
| `ate` | date (YYYY-MM-DD) | Não | Fim do período de publicação |
| `pagina` | integer | Não | Página, base 0. Padrão: 0 |
| `tamanho` | integer | Não | Itens por página, max 100. Padrão: 20 |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/analise/mencoes" \
  --data-urlencode "nrPartido=13" \
  --data-urlencode "sentimento=negativo" \
  --data-urlencode "pagina=0" \
  --data-urlencode "tamanho=20"
```

```json
{
  "content": [
    {
      "idClassificacao": 918273,
      "paragrafo": "O partido enfrenta criticas apos o anuncio da nova coligacao...",
      "linkNoticia": "https://exemplo.com/noticia/123",
      "idMidia": 7,
      "nomeMidia": "G1",
      "dataPublicacao": "2026-09-10T08:30:00",
      "sentimento": "negativo",
      "scoreConfianca": 0.86,
      "metodo": "lexicon-baseline-v1",
      "pipelineVersion": "1.3.0",
      "idArtigoScrape": 445566
    }
  ],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 20,
  "size": 20,
  "totalElements": 1300,
  "totalPages": 65
}
```

---

## Análise por matéria

Análise no nível da matéria inteira (não do parágrafo): resumo, tema e panorama mensal. Lê tabelas diferentes das rotas de `/api/analise/*` acima — são produzidas por um componente separado (o analyser). Mesma regra de alvo: `tituloEleitoral` ou `nrPartido`, nunca os dois.

### `GET /api/analise/materias` — Matérias analisadas sobre a entidade · paginado

A unidade é a matéria, não o parágrafo. Aceita filtro por tema, por janela mensal e por período. Não há campo de título — o analyser não o persiste; use `linkNoticia` para referenciar a matéria.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Ver nota | Título eleitoral do político-alvo |
| `nrPartido` | integer | Ver nota | Número de legenda do partido-alvo |
| `tema` | string | Não | Filtra pelo tema da matéria, ex. `saude` |
| `janela` | string (YYYY-MM) | Não | Janela mensal, ex. `2026-08` |
| `de` | date (YYYY-MM-DD) | Não | Início do período de publicação |
| `ate` | date (YYYY-MM-DD) | Não | Fim do período de publicação |
| `pagina` | integer | Não | Página, base 0. Padrão: 0 |
| `tamanho` | integer | Não | Itens por página, max 100. Padrão: 20 |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/analise/materias" \
  --data-urlencode "nrPartido=13" \
  --data-urlencode "janela=2026-08"
```

```json
{
  "content": [
    {
      "itemId": "art_1__partido_13",
      "articleId": 998877,
      "rotulo": "negativo",
      "resumo": "A materia discute a repercussao da nova coligacao do partido.",
      "temas": ["politica", "eleicoes"],
      "linkNoticia": "https://exemplo.com/noticia/456",
      "idMidia": 7,
      "nomeMidia": "G1",
      "dataPublicacao": "2026-08-14T09:00:00Z",
      "janela": "2026-08",
      "modelo": "gemma-3-27b",
      "regraAgregacao": "eixo-por-sinal-v2",
      "status": "consolidado"
    }
  ],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 20,
  "size": 20,
  "totalElements": 340,
  "totalPages": 17
}
```

### `GET /api/analise/por-tema` — Matérias abertas por tema

Concentração de pauta: se quase toda a cobertura de uma entidade cai num único tema, o agregado descreve aquele recorte, não a cobertura em geral. Matérias sem tema atribuído entram como grupo próprio (`semTema`). Uma matéria com vários temas conta em cada um, então a soma de `temas[].total` pode passar de `totalMaterias`.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Ver nota | Título eleitoral do político-alvo |
| `nrPartido` | integer | Ver nota | Número de legenda do partido-alvo |
| `de` | date (YYYY-MM-DD) | Não | Início do período de publicação |
| `ate` | date (YYYY-MM-DD) | Não | Fim do período de publicação |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/analise/por-tema" \
  --data-urlencode "nrPartido=13"
```

```json
{
  "tipo": "partido",
  "identificador": "13",
  "nome": "PARTIDO DOS TRABALHADORES",
  "totalMaterias": 340,
  "semTema": 28,
  "temas": [
    { "tema": "politica", "total": 210, "percentual": 61.76 },
    { "tema": "eleicoes", "total": 150, "percentual": 44.12 }
  ],
  "proveniencia": [
    { "modelo": "gemma-3-27b", "regraAgregacao": "eixo-por-sinal-v2", "total": 340 }
  ]
}
```

### `GET /api/analise/panorama` — Panorama consolidado da entidade no mês

O texto que o analyser consolidou para cada eixo (polaridade: positivo/negativo/neutro) da janela, com temas e fontes já resolvidas para veículo e link. Sem `janela`, responde o mês mais recente que tiver panorama. Um eixo sem panorama simplesmente não aparece na lista de `eixos`.

| Parâmetro | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `tituloEleitoral` | string | Ver nota | Título eleitoral do político-alvo |
| `nrPartido` | integer | Ver nota | Número de legenda do partido-alvo |
| `janela` | string (YYYY-MM) | Não | Janela mensal. Ausente = mês mais recente disponível |

```bash
curl -G -H "X-API-KEY: SUA_CHAVE_AQUI" \
  "http://localhost:8080/api/analise/panorama" \
  --data-urlencode "nrPartido=13" \
  --data-urlencode "janela=2026-08"
```

```json
{
  "tipo": "partido",
  "identificador": "13",
  "nome": "PARTIDO DOS TRABALHADORES",
  "janela": "2026-08",
  "eixos": [
    {
      "eixo": "negativo",
      "versao": 3,
      "totalItens": 58,
      "texto": "Ao longo de agosto, a cobertura sobre o partido concentrou criticas relacionadas a formacao de coligacoes regionais...",
      "temas": [
        {
          "tema": "eleicoes",
          "ocorrencias": 22,
          "fontes": [
            {
              "itemId": "art_1__partido_13",
              "veiculo": "G1",
              "url": "https://exemplo.com/noticia/456",
              "dataPublicacao": "2026-08-14T09:00:00Z"
            }
          ]
        }
      ],
      "fontes": [
        {
          "itemId": "art_1__partido_13",
          "veiculo": "G1",
          "url": "https://exemplo.com/noticia/456",
          "dataPublicacao": "2026-08-14T09:00:00Z"
        }
      ],
      "avisos": [],
      "modelo": "gemma-3-27b",
      "criadoEm": "2026-09-01T03:00:00Z"
    }
  ]
}
```
