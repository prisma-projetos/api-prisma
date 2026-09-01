package br.com.prisma.api_prisma.dto;

import br.com.prisma.api_prisma.model.Partido;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partido politico")
public record PartidoResponse(
        @Schema(example = "13") Integer numero,
        @Schema(example = "PT") String sigla,
        String nome,
        String descricao) {

    public static PartidoResponse from(Partido p) {
        return new PartidoResponse(p.getNumero(), p.getSigla(), p.getNome(), p.getDescricao());
    }
}
