package br.com.prisma.api_prisma.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Politico com seu historico de candidaturas. */
@Schema(description = "Politico e suas candidaturas")
public record PoliticoDetalheResponse(
        PoliticoResponse politico,
        List<CandidaturaResponse> candidaturas) {
}
