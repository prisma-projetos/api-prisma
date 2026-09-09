package br.com.prisma.api_prisma.dto;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.prisma.api_prisma.model.AnaliseItem;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma materia analisada, com o link da noticia de origem.
 *
 * <p>Carrega sempre {@code modelo} e {@code regraAgregacao}: o resumo e saida
 * de modelo e nao circula sem dizer quem o escreveu.
 *
 * <p><b>Nao ha titulo.</b> O analyser nao persiste titulo de materia -- ver
 * {@link AnaliseItem}. Um campo sempre nulo seria pior que a ausencia dele,
 * porque prometeria ao frontend um dado que nunca vai chegar. Para exibir a
 * materia, o caminho e {@code linkNoticia}.
 */
@Schema(description = "Materia analisada, com resumo, temas e proveniencia")
public record AnaliseMateriaResponse(
        @Schema(description = "Id do item no analyser. E tambem o id de fonte citado "
                + "pelo panorama", example = "art_1__politico_122418060191")
        String itemId,
        @Schema(description = "Id do artigo no banco do scraper (externo a este banco)")
        Long articleId,
        @Schema(description = "Sentimento da materia inteira. O analyser chama este "
                + "campo de 'eixo'", example = "negativo")
        String rotulo,
        @Schema(description = "Resumo da materia, escrito por modelo -- ver 'modelo'. "
                + "Nulo enquanto a materia nao foi resumida")
        String resumo,
        @Schema(description = "Recortes tematicos da materia", example = "[\"saude\"]")
        List<String> temas,
        String linkNoticia,
        Integer idMidia,
        String nomeMidia,
        OffsetDateTime dataPublicacao,
        @Schema(description = "Janela mensal da materia", example = "2026-08") String janela,
        String modelo,
        String regraAgregacao,
        @Schema(description = "novo, resumido ou consolidado") String status) {

    public static AnaliseMateriaResponse from(AnaliseItem i, List<String> temas) {
        var midia = i.getMidia();
        return new AnaliseMateriaResponse(
                i.getId(), i.getArticleId(), i.getRotulo(), i.getResumo(), temas,
                i.getLinkNoticia(),
                midia != null ? midia.getId() : null,
                midia != null ? midia.getNome() : null,
                i.getDataPublicacao(), i.getJanela(), i.getModelo(),
                i.getRegraAgregacao(), i.getStatus());
    }
}
