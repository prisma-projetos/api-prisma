package br.com.prisma.api_prisma.dto;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O texto consolidado sobre uma entidade numa janela mensal, por eixo.
 *
 * <p>E o produto final do {@code analyser-prisma}: um paragrafo por eixo,
 * escrito por modelo a partir dos resumos das materias daquele mes.
 *
 * <p><b>Eixo aqui e polaridade</b> -- {@code positivo}, {@code negativo} ou
 * {@code neutro} --, nao recorte tematico. O nome vem do analyser e foi
 * mantido para nao criar um terceiro vocabulario entre banco, API e frontend.
 *
 * <p>Cada eixo vem sempre na maior versao disponivel; as anteriores ficam no
 * banco para auditoria e nao sao expostas. Um eixo sem panorama simplesmente
 * nao aparece na lista -- ausencia e informacao, nao erro.
 */
@Schema(description = "Panorama consolidado da entidade numa janela mensal")
public record PanoramaResponse(
        @Schema(example = "politico") String tipo,
        String identificador,
        String nome,
        @Schema(description = "Janela mensal consolidada", example = "2026-08") String janela,
        List<PanoramaEixo> eixos) {

    /** O panorama de um eixo. */
    @Schema(description = "Panorama de um eixo (polaridade)")
    public record PanoramaEixo(
            @Schema(example = "negativo") String eixo,
            int versao,
            int totalItens,
            @Schema(description = "O paragrafo consolidado que o cidadao le") String texto,
            List<TemaPanorama> temas,
            @Schema(description = "Materias que sustentam o panorama") List<Fonte> fontes,
            @Schema(description = "Divergencias que a verificacao registrou sem corrigir")
            List<String> avisos,
            String modelo,
            OffsetDateTime criadoEm) {
    }

    /** Tema recorrente, com as materias que o sustentam. */
    @Schema(description = "Tema recorrente do panorama")
    public record TemaPanorama(String tema, int ocorrencias, List<Fonte> fontes) {
    }

    /**
     * Uma materia citada pelo panorama, ja resolvida.
     *
     * <p>O analyser guarda so o {@code itemId}; resolver para veiculo e link e
     * trabalho desta API.
     *
     * <p>Sem titulo, pela mesma regra que {@link AnaliseMateriaResponse} segue:
     * o analyser nao persiste titulo de materia, e um campo eternamente nulo
     * prometeria ao frontend um dado que nunca vai chegar. Para rotular a
     * fonte na tela, o caminho e {@code veiculo} com {@code dataPublicacao}.
     */
    @Schema(description = "Materia que sustenta uma afirmacao do panorama")
    public record Fonte(
            String itemId,
            String veiculo,
            String url,
            OffsetDateTime dataPublicacao) {
    }
}
