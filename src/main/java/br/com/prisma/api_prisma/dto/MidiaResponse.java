package br.com.prisma.api_prisma.dto;

import br.com.prisma.api_prisma.model.Midia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Veiculo de imprensa")
public record MidiaResponse(Integer id, String nome) {

    public static MidiaResponse from(Midia m) {
        return new MidiaResponse(m.getId(), m.getNome());
    }
}
