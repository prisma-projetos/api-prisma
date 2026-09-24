package br.com.prisma.api_prisma.dto;

import br.com.prisma.api_prisma.model.Candidatura;
import io.swagger.v3.oas.annotations.media.Schema;

/** Uma candidatura: e aqui que cargo, UF e partido fazem sentido. */
@Schema(description = "Candidatura de um politico em uma eleicao")
public record CandidaturaResponse(
        Long sequencial,
        Integer anoEleicao,
        Integer turno,
        String cargo,
        String uf,
        String unidadeEleitoral,
        Integer numeroUrna,
        String nomeUrna,
        Integer nrPartido,
        String siglaPartido,
        String situacaoCandidatura,
        String resultado,
        String coligacao,
        String federacao,
        @Schema(description = "URL da foto do candidato no MinIO. Nula se UF ou "
                + "sequencial nao existirem.",
                example = "https://api-minio.ajvale.com.br/2026/fotosCandidatos/AC/FAC10002532416_div.jpg")
        String urlFoto) {

    public static CandidaturaResponse from(Candidatura c) {
        var eleicao = c.getEleicao();
        var partido = c.getPartido();
        return new CandidaturaResponse(
                c.getSequencial(),
                eleicao != null ? eleicao.getAno() : null,
                eleicao != null ? eleicao.getTurno() : null,
                c.getCargo(), c.getUf(), c.getUnidadeEleitoral(),
                c.getNumeroUrna(), c.getNomeUrna(),
                partido != null ? partido.getNumero() : null,
                partido != null ? partido.getSigla() : null,
                c.getSituacaoCandidatura(), c.getSituacaoTurno(),
                c.getColigacao(), c.getFederacao(),
                MinioUrls.fotoCandidato(c.getUf(), c.getSequencial()));
    }
}
