package br.com.prisma.api_prisma.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Parametros da consulta, enviados na query string.
 *
 * <p>Informe {@code tituloEleitoral} para um politico OU {@code nrPartido}
 * para um partido -- nunca os dois.
 *
 * <p>Ja foi corpo de um {@code GET}, que o {@code fetch} do navegador e do
 * Node nao sabem enviar. Os nomes dos campos sao os mesmos de antes, e agora
 * sao os nomes dos parametros.
 *
 * <p>Parametro vazio ({@code ?janela=}) vale como ausente, igual a um campo
 * vazio no corpo valia: por isso os padroes abaixo aceitam a string vazia.
 * Paginacao ausente fica {@code null} e o servico aplica o padrao.
 */
@Schema(description = "Parametros da consulta, na query string")
public record ConsultaRequest(

        // String, nunca numero: o titulo eleitoral pode comecar com zero.
        @Schema(description = "Numero do titulo eleitoral do politico", example = "123456789012")
        String tituloEleitoral,

        @Schema(description = "Numero de legenda do partido", example = "13")
        Integer nrPartido,

        @Schema(description = "Filtra por sentimento: positivo, neutro ou negativo",
                example = "negativo", allowableValues = { "positivo", "neutro", "negativo" })
        @Pattern(regexp = "^(?i)(positivo|neutro|negativo)?$",
                message = "deve ser 'positivo', 'neutro' ou 'negativo'")
        String sentimento,

        @Schema(description = "Filtra por tema da materia. So vale para "
                + "/analise/materias", example = "saude")
        String tema,

        @Schema(description = "Janela mensal YYYY-MM. Em /analise/panorama, ausente "
                + "significa a janela mais recente com material", example = "2026-08",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])$")
        @Pattern(regexp = "^(\\d{4}-(0[1-9]|1[0-2]))?$",
                message = "deve estar no formato YYYY-MM")
        String janela,

        @Schema(description = "Inicio do periodo de publicacao", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate de,

        @Schema(description = "Fim do periodo de publicacao", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate ate,

        @Schema(description = "Pagina (base 0)", example = "0", defaultValue = "0",
                minimum = "0")
        @Min(value = 0, message = "deve ser maior ou igual a 0")
        Integer pagina,

        @Schema(description = "Itens por pagina (max 100)", example = "20", defaultValue = "20",
                minimum = "1", maximum = "100")
        @Min(value = 1, message = "deve estar entre 1 e 100")
        @Max(value = 100, message = "deve estar entre 1 e 100")
        Integer tamanho) {
}
