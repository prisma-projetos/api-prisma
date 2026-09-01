package br.com.prisma.api_prisma.dto;

import java.time.LocalDate;

import br.com.prisma.api_prisma.model.Politico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Politico, na granularidade da pessoa")
public record PoliticoResponse(
        @Schema(description = "Identificador estavel do TSE") String tituloEleitoral,
        String nome,
        String nomeSocial,
        LocalDate dataNascimento,
        String ufNascimento,
        String genero,
        String grauInstrucao,
        String corRaca,
        String ocupacao,
        String descricao) {

    public static PoliticoResponse from(Politico p) {
        return new PoliticoResponse(
                p.getTituloEleitoral(), p.getNome(), p.getNomeSocial(),
                p.getDataNascimento(), p.getUfNascimento(), p.getGenero(),
                p.getGrauInstrucao(), p.getCorRaca(), p.getOcupacao(), p.getDescricao());
    }
}
