package br.com.prisma.api_prisma.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sentimento aberto por veiculo.
 *
 * <p>Serve para auditar vies de selecao de fontes: se quase todo o volume de
 * uma entidade vem de um unico veiculo, o agregado descreve a linha editorial
 * daquele veiculo, nao a cobertura em geral.
 */
@Schema(description = "Sentimento sobre uma entidade, aberto por veiculo")
public record SentimentoPorMidiaResponse(
        String tipoEntidade,
        String identificador,
        String nome,
        @Schema(description = "Quantidade de veiculos distintos identificados")
        int veiculosDistintos,
        long totalParagrafos,
        @Schema(description = "Paragrafos sem veiculo associado no banco. Nao entram no "
                + "recorte por veiculo, mas contam no total -- ignora-los faria a cobertura "
                + "parecer menor do que e")
        long paragrafosSemVeiculo,
        List<MidiaSentimento> midias) {

    @Schema(description = "Volume de um veiculo, aberto por sentimento")
    public record MidiaSentimento(
            Integer idMidia,
            String nomeMidia,
            long total,
            @Schema(description = "Participacao do veiculo no total, 0 a 100")
            double percentualDoTotal,
            long positivo,
            long neutro,
            long negativo) {
    }
}
