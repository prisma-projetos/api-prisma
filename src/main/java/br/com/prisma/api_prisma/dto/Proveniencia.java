package br.com.prisma.api_prisma.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metodo e versao do pipeline que produziram um conjunto de resultados.
 *
 * <p>Vive solto no pacote, e nao dentro de uma resposta especifica, porque a
 * regra vale para toda a API: nenhum numero circula sem o modelo que o gerou.
 * Sentimento por paragrafo e analise por materia sao produzidos por
 * componentes diferentes, mas prestam a mesma conta.
 */
@Schema(description = "Metodo e versao do pipeline que geraram os resultados")
public record Proveniencia(String metodo, String pipelineVersion, long total) {
}
