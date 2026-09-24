package br.com.prisma.api_prisma.dto;

import br.com.prisma.api_prisma.model.Partido;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partido politico")
public record PartidoResponse(
        @Schema(example = "13") Integer numero,
        @Schema(example = "PT") String sigla,
        String nome,
        String descricao,
        @Schema(description = "URL do logo do partido no MinIO. Nulo se a sigla nao existir.",
                example = "https://api-minio.ajvale.com.br/2026/fotosPartidos/PT.svg")
        String urlLogo) {

    public static PartidoResponse from(Partido p) {
        return new PartidoResponse(p.getNumero(), p.getSigla(), p.getNome(), p.getDescricao(),
                MinioUrls.logoPartido(p.getSigla()));
    }
}
