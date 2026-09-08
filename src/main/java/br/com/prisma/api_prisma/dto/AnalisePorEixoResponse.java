package br.com.prisma.api_prisma.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Volume de materias por eixo tematico.
 *
 * <p>Serve ao mesmo proposito que {@link SentimentoPorMidiaResponse} cumpre
 * para veiculos: se quase toda a cobertura de uma entidade esta num unico
 * eixo, o agregado descreve aquele recorte, nao a cobertura em geral.
 */
@Schema(description = "Distribuicao das materias por eixo tematico")
public record AnalisePorEixoResponse(
        @Schema(example = "partido") String tipo,
        String identificador,
        String nome,
        long totalMaterias,
        @Schema(description = "Materias sem eixo atribuido pelo analisador")
        long semEixo,
        List<EixoContagem> eixos,
        @Schema(description = "Metodo e versao do pipeline que geraram as analises")
        List<SentimentoResumoResponse.Proveniencia> proveniencia) {

    @Schema(description = "Volume de um eixo")
    public record EixoContagem(String eixo, long total, double percentual) {
    }
}
