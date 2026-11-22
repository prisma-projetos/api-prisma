package br.com.prisma.api_prisma.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Volume de materias por tema.
 *
 * <p>Serve ao mesmo proposito que {@link SentimentoPorMidiaResponse} cumpre
 * para veiculos: se quase toda a cobertura de uma entidade esta num unico
 * tema, o agregado descreve aquele recorte, nao a cobertura em geral.
 *
 * <p><b>Tema, nao eixo.</b> No analyser, "eixo" e polaridade de sentimento
 * ({@code positivo}/{@code negativo}/{@code neutro}), nao recorte de pauta. A
 * concentracao de pauta se mede pelos temas, e e isso que este recorte faz.
 * Para a distribuicao por polaridade no nivel da materia, o campo e
 * {@code rotulo} em {@link AnaliseMateriaResponse}.
 *
 * <p>Uma materia com varios temas conta em cada um deles -- portanto a soma
 * de {@code total} pode passar de {@code totalMaterias}. O percentual e sobre
 * as materias, para responder "que fatia da cobertura toca este tema".
 */
@Schema(description = "Distribuicao das materias por tema")
public record AnalisePorTemaResponse(
        @Schema(example = "partido") String tipo,
        String identificador,
        String nome,
        long totalMaterias,
        @Schema(description = "Materias sem tema atribuido pelo analisador")
        long semTema,
        List<TemaContagem> temas,
        @Schema(description = "Modelo e regra que geraram as analises")
        List<ProvenienciaAnalise> proveniencia) {

    @Schema(description = "Volume de um tema")
    public record TemaContagem(String tema, long total, double percentual) {
    }
}
