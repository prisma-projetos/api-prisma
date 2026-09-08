package br.com.prisma.api_prisma.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.AnaliseMateria;

/**
 * Consultas sobre as materias analisadas.
 *
 * <p>Segue o mesmo contrato de {@link ClassificacaoRepository}: o alvo entra
 * por sentinela, e nao por null. O Postgres infere {@code bytea} para um NULL
 * sem tipo e a comparacao falha em tempo de execucao -- entao "sem filtro"
 * viaja como valor concreto ({@code ""} e {@code -1}), normalizado no servico.
 */
public interface AnaliseMateriaRepository extends JpaRepository<AnaliseMateria, Integer> {

    /**
     * Restringe ao politico ou ao partido informado, conforme o sentinela.
     *
     * <p><b>Nao troque {@code tituloEleitoral} / {@code numero} por outro campo
     * da entidade associada.</b> Os dois sao a <i>chave primaria</i> de
     * {@code Politico} e {@code Partido}, entao o Hibernate resolve o caminho
     * direto na coluna de FK desta tabela e o SQL sai sem join nenhum. Navegar
     * para qualquer campo nao-identificador (por exemplo {@code politico.nome})
     * geraria um <b>inner join implicito</b> nas duas associacoes ao mesmo
     * tempo -- e como cada linha tem so uma delas preenchida, a query passaria
     * a devolver vazio em silencio.
     */
    String FILTRO_ENTIDADE = """
              and ((:titulo <> '' and a.politico.tituloEleitoral = :titulo)
                or (:numero <> -1 and a.partido.numero = :numero))
            """;

    /** Recorte por data de publicacao da materia. */
    String FILTRO_PERIODO = """
              and a.dataPublicacao between :inicio and :fim
            """;

    /** Quantas materias por eixo tematico. */
    interface ContagemPorEixo {
        String getEixo();

        long getTotal();
    }

    /** Quantas analises cada modelo produziu. */
    interface ContagemPorModelo {
        String getMetodo();

        String getPipelineVersion();

        long getTotal();
    }

    /** {@code eixo} vazio traz todos os eixos. */
    @Query("""
            select a from AnaliseMateria a
              left join fetch a.midia
            where (:eixo = '' or a.eixo = :eixo)
            """ + FILTRO_ENTIDADE + FILTRO_PERIODO + """
            order by a.dataPublicacao desc
            """)
    Page<AnaliseMateria> buscarMaterias(@Param("titulo") String titulo,
                                        @Param("numero") int numero,
                                        @Param("eixo") String eixo,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim,
                                        Pageable pageable);

    /**
     * Volume por eixo. O agrupamento mantem o eixo nulo como grupo proprio:
     * materia sem eixo atribuido e informacao, nao lixo a ser descartado em
     * silencio -- mesma decisao do left join por midia na classificacao.
     */
    @Query("""
            select a.eixo as eixo,
                   count(a) as total
            from AnaliseMateria a
            where 1 = 1
            """ + FILTRO_ENTIDADE + FILTRO_PERIODO + """
            group by a.eixo
            """)
    List<ContagemPorEixo> contarPorEixo(@Param("titulo") String titulo,
                                        @Param("numero") int numero,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim);

    @Query("""
            select a.metodo as metodo,
                   a.pipelineVersion as pipelineVersion,
                   count(a) as total
            from AnaliseMateria a
            where 1 = 1
            """ + FILTRO_ENTIDADE + """
            group by a.metodo, a.pipelineVersion
            """)
    List<ContagemPorModelo> contarPorModelo(@Param("titulo") String titulo,
                                            @Param("numero") int numero);
}
