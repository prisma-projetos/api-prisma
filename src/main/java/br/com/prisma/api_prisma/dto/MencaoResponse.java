package br.com.prisma.api_prisma.dto;

import java.time.LocalDateTime;

import br.com.prisma.api_prisma.model.Classificacao;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um paragrafo analisado, com o link da noticia de origem.
 *
 * <p>E o nivel de rastreabilidade que este banco permite: da classificacao
 * chega-se ao texto exato e a materia publicada.
 */
@Schema(description = "Paragrafo analisado e sua classificacao")
public record MencaoResponse(
        Integer idClassificacao,
        @Schema(description = "Trecho exato que originou a classificacao") String paragrafo,
        String linkNoticia,
        Integer idMidia,
        String nomeMidia,
        LocalDateTime dataPublicacao,
        @Schema(example = "negativo") String sentimento,
        Double scoreConfianca,
        @Schema(description = "Metodo do classificador", example = "lexicon-baseline-v1")
        String metodo,
        String pipelineVersion,
        @Schema(description = "Id do artigo no banco do scraper (externo a este banco)")
        Long idArtigoScrape) {

    public static MencaoResponse from(Classificacao c) {
        var midia = c.getMidia();
        return new MencaoResponse(
                c.getId(), c.getParagrafo(), c.getLinkNoticia(),
                midia != null ? midia.getId() : null,
                midia != null ? midia.getNome() : null,
                c.getDataPublicacao(), c.getSentimento(), c.getScoreConfianca(),
                c.getMetodo(), c.getPipelineVersion(), c.getIdArtigoScrape());
    }
}
