package br.com.prisma.api_prisma.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.MencaoResponse;
import br.com.prisma.api_prisma.dto.SentimentoPorMidiaResponse;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse;
import br.com.prisma.api_prisma.service.AnaliseQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Analise das noticias. Todas as consultas aceitam {@code tituloEleitoral}
 * (politico) OU {@code nrPartido} (partido) na query string, nunca os dois.
 */
@RestController
@RequestMapping("/api/analise")
@Tag(name = "Analise", description = "Sentimento agregado sobre politicos e partidos na imprensa")
public class AnaliseController {

    private final AnaliseQueryService service;

    public AnaliseController(AnaliseQueryService service) {
        this.service = service;
    }

    @GetMapping("/sentimento")
    @Operation(summary = "Sentimento agregado sobre a entidade",
            description = "A unidade contada e o paragrafo, nao a noticia. A resposta traz o "
                    + "metodo e a versao do pipeline que produziram os numeros.")
    public SentimentoResumoResponse sentimento(@ParameterObject @Valid ConsultaRequest consulta) {
        return service.resumoSentimento(consulta);
    }

    @GetMapping("/por-midia")
    @Operation(summary = "Sentimento aberto por veiculo",
            description = "Expoe concentracao de fontes: se quase todo o volume vem de um "
                    + "unico veiculo, o agregado descreve a linha editorial dele, nao a "
                    + "cobertura em geral.")
    public SentimentoPorMidiaResponse porMidia(@ParameterObject @Valid ConsultaRequest consulta) {
        return service.sentimentoPorMidia(consulta);
    }

    @GetMapping("/mencoes")
    @Operation(summary = "Paragrafos analisados",
            description = "Rastreabilidade: o trecho exato que originou cada classificacao e o "
                    + "link da noticia. Aceita filtro por sentimento e por periodo.")
    public Page<MencaoResponse> mencoes(@ParameterObject @Valid ConsultaRequest consulta) {
        return service.mencoes(consulta);
    }
}
