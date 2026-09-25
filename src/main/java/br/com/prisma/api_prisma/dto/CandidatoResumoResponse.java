package br.com.prisma.api_prisma.dto;

import br.com.prisma.api_prisma.model.Candidatura;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Linha da listagem de candidatos de um partido: so o minimo que a pagina do
 * partido precisa pra montar os cards. Dados completos (nascimento,
 * historico) vivem em {@code /api/politicos/detalhe}.
 *
 * <p>{@code cargo} vem junto porque a listagem so faz sentido por
 * candidatura, nao por pessoa: e o cargo que o filtro {@code ?cargo=} usa
 * para categorizar os candidatos na tela.
 */
@Schema(description = "Candidato, resumido para listagem")
public record CandidatoResumoResponse(
        String tituloEleitoral,
        String nome,
        @Schema(description = "Cargo ao qual o candidato concorre nesta "
                + "candidatura", example = "PRESIDENTE")
        String cargo,
        @Schema(description = "URL da foto do candidato no MinIO. Nula se UF ou "
                + "sequencial nao existirem.",
                example = "https://api-minio.ajvale.com.br/2026/fotosCandidatos/MG/FMG130002542033_div.jpg")
        String urlFoto) {

    public static CandidatoResumoResponse from(Candidatura c) {
        var politico = c.getPolitico();
        return new CandidatoResumoResponse(
                politico != null ? politico.getTituloEleitoral() : null,
                politico != null ? politico.getNome() : null,
                c.getCargo(),
                MinioUrls.fotoCandidato(c.getUf(), c.getSequencial()));
    }
}
