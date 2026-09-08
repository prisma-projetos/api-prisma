package br.com.prisma.api_prisma.dto;

import java.time.LocalDateTime;

import br.com.prisma.api_prisma.model.AnaliseMateria;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma materia analisada, com o link da noticia de origem.
 *
 * <p>Carrega sempre {@code metodo} e {@code pipelineVersion}: titulo e resumo
 * podem ser saida de modelo, e nao circulam sem dizer quem os produziu.
 */
@Schema(description = "Materia analisada, com eixo, resumo e proveniencia")
public record AnaliseMateriaResponse(
        Integer idAnalise,
        @Schema(description = "Recorte tematico da materia", example = "saude") String eixo,
        String titulo,
        @Schema(description = "Resumo da materia. Pode ser gerado por modelo -- ver metodo")
        String resumo,
        String linkNoticia,
        Integer idMidia,
        String nomeMidia,
        LocalDateTime dataPublicacao,
        @Schema(description = "Metodo do analisador") String metodo,
        String pipelineVersion,
        @Schema(description = "Id do artigo no banco do scraper (externo a este banco)")
        Long idArtigoScrape) {

    public static AnaliseMateriaResponse from(AnaliseMateria a) {
        var midia = a.getMidia();
        return new AnaliseMateriaResponse(
                a.getId(), a.getEixo(), a.getTitulo(), a.getResumo(), a.getLinkNoticia(),
                midia != null ? midia.getId() : null,
                midia != null ? midia.getNome() : null,
                a.getDataPublicacao(), a.getMetodo(), a.getPipelineVersion(),
                a.getIdArtigoScrape());
    }
}
