package br.com.prisma.api_prisma.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sentimento agregado dos paragrafos que citam uma entidade.
 *
 * <p>A unidade contada e o paragrafo, nao a noticia: uma materia longa pesa
 * mais que uma nota curta. {@code proveniencia} existe para que o numero nunca
 * seja lido sem o modelo que o produziu.
 */
@Schema(description = "Sentimento agregado sobre uma entidade em um periodo")
public record SentimentoResumoResponse(
        @Schema(description = "politico ou partido") String tipoEntidade,
        @Schema(description = "Titulo eleitoral ou numero do partido") String identificador,
        String nome,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        @Schema(description = "Total de paragrafos analisados") long totalParagrafos,
        List<ContagemSentimento> porSentimento,
        @Schema(description = "Modelos que produziram estes numeros")
        List<Proveniencia> proveniencia) {

    @Schema(description = "Volume e confianca de um sentimento")
    public record ContagemSentimento(
            @Schema(example = "negativo") String sentimento,
            long total,
            @Schema(description = "Percentual do total, 0 a 100") double percentual,
            @Schema(description = "Score medio de confianca do modelo") Double scoreMedio) {
    }

    @Schema(description = "Metodo e versao do pipeline que geraram as classificacoes")
    public record Proveniencia(String metodo, String pipelineVersion, long total) {
    }
}
