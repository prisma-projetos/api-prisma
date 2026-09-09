package br.com.prisma.api_prisma.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parametros da consulta, enviados no corpo da requisicao.
 *
 * <p>Informe {@code tituloEleitoral} para um politico OU {@code nrPartido}
 * para um partido -- nunca os dois.
 */
@Schema(description = "Parametros da consulta, no corpo da requisicao")
public record ConsultaRequest(

        @Schema(description = "Numero do titulo eleitoral do politico", example = "123456789012")
        String tituloEleitoral,

        @Schema(description = "Numero de legenda do partido", example = "13")
        Integer nrPartido,

        @Schema(description = "Filtra por sentimento: positivo, neutro ou negativo",
                example = "negativo")
        String sentimento,

        @Schema(description = "Filtra por tema da materia. So vale para "
                + "/analise/materias", example = "saude")
        String tema,

        @Schema(description = "Janela mensal YYYY-MM. Em /analise/panorama, ausente "
                + "significa a janela mais recente com material", example = "2026-08")
        String janela,

        @Schema(description = "Inicio do periodo de publicacao", example = "2026-01-01")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate de,

        @Schema(description = "Fim do periodo de publicacao", example = "2026-12-31")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate ate,

        @Schema(description = "Pagina (base 0)", example = "0")
        Integer pagina,

        @Schema(description = "Itens por pagina (max 100)", example = "20")
        Integer tamanho) {
}
