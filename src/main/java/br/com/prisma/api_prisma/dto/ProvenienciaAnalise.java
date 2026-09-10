package br.com.prisma.api_prisma.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Quem produziu um conjunto de analises de materia.
 *
 * <p>Cumpre o mesmo dever que {@link Proveniencia} cumpre no nivel do
 * paragrafo -- nenhum numero circula sem o modelo que o gerou --, mas com os
 * campos que o analyser realmente grava. Sao dois produtores por linha, e nao
 * um: o {@code modelo} escreve o resumo, a {@code regraAgregacao} decide o
 * rotulo. Reaproveitar {@code Proveniencia} exigiria chamar a regra de
 * "pipelineVersion", que ela nao e -- {@code analyser_item} nao tem coluna de
 * pipeline_version, ela vive no evento de entrada.
 */
@Schema(description = "Modelo e regra que geraram as analises de materia")
public record ProvenienciaAnalise(
        @Schema(description = "Modelo que escreveu os resumos. Nulo em materias ainda "
                + "nao resumidas", example = "gemma-3-27b")
        String modelo,
        @Schema(description = "Regra que decidiu o rotulo da materia",
                example = "eixo-por-sinal-v2")
        String regraAgregacao,
        long total) {
}
