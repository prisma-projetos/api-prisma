package br.com.prisma.api_prisma.service;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.model.Partido;
import br.com.prisma.api_prisma.model.Politico;
import br.com.prisma.api_prisma.repository.ClassificacaoRepository;

/**
 * Entidade alvo de uma consulta de analise: um politico ou um partido, nunca
 * os dois -- a mesma restricao que o banco impoe em {@code entidade_matching}.
 *
 * <p>Concentrar aqui a traducao para os parametros do repositorio evita que
 * cada consulta repita um {@code if (politico) ... else ...}.
 */
public record Alvo(String tipo, String identificador, String nome,
                   String tituloEleitoral, Integer nrPartido) {

    public static final String TIPO_POLITICO = "politico";
    public static final String TIPO_PARTIDO = "partido";

    public static Alvo de(Politico politico) {
        return new Alvo(TIPO_POLITICO,
                politico.getTituloEleitoral(),
                politico.getNome(),
                politico.getTituloEleitoral(),
                null);
    }

    public static Alvo de(Partido partido) {
        return new Alvo(TIPO_PARTIDO,
                String.valueOf(partido.getNumero()),
                partido.getSigla(),
                null,
                partido.getNumero());
    }

    /**
     * Resolve o alvo a partir dos parametros da consulta, exigindo exatamente um
     * identificador. Vive aqui, e nao em cada servico, para que toda consulta
     * -- sentimento, mencoes ou analise de materia -- recuse os dois
     * identificadores juntos com a mesma mensagem.
     */
    static Alvo resolver(ConsultaRequest request, CatalogoQueryService catalogo) {
        boolean temPolitico = request.tituloEleitoral() != null
                && !request.tituloEleitoral().isBlank();
        boolean temPartido = request.nrPartido() != null;

        if (temPolitico == temPartido) {
            throw new IllegalArgumentException(
                    "Informe 'tituloEleitoral' (politico) ou 'nrPartido' (partido) -- "
                            + "exatamente um dos dois.");
        }
        return temPolitico
                ? de(catalogo.carregarPolitico(request.tituloEleitoral()))
                : de(catalogo.carregarPartido(request.nrPartido()));
    }

    /** Titulo eleitoral, ou o sentinela que desliga o filtro por politico. */
    public String tituloParaConsulta() {
        return tituloEleitoral != null ? tituloEleitoral : ClassificacaoRepository.SEM_POLITICO;
    }

    /** Numero do partido, ou o sentinela que desliga o filtro por partido. */
    public int numeroParaConsulta() {
        return nrPartido != null ? nrPartido : ClassificacaoRepository.SEM_PARTIDO;
    }
}
