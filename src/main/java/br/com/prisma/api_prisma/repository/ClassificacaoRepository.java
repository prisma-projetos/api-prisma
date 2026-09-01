package br.com.prisma.api_prisma.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.Classificacao;

/**
 * Consultas sobre os paragrafos analisados.
 *
 * <p>Toda consulta aqui e sobre um politico OU um partido. Em vez de duplicar
 * cada query nas duas versoes, o alvo entra por {@link #FILTRO_ENTIDADE}: quem
 * chama preenche o identificador que tem e passa o sentinela no outro.
 *
 * <p><b>Por que sentinelas e nao null:</b> o Postgres infere {@code bytea} para
 * um NULL sem tipo, e a comparacao falha em tempo de execucao. Entao "sem
 * filtro" viaja como valor concreto -- {@code ""} e {@code -1} -- normalizados
 * no servico.
 */
public interface ClassificacaoRepository extends JpaRepository<Classificacao, Integer> {

    /** Identificador ausente de politico. */
    String SEM_POLITICO = "";

    /** Identificador ausente de partido. Nenhuma legenda real e negativa. */
    int SEM_PARTIDO = -1;

    /** Restringe ao politico ou ao partido informado, conforme o sentinela. */
    String FILTRO_ENTIDADE = """
              and ((:titulo <> '' and c.politico.tituloEleitoral = :titulo)
                or (:numero <> -1 and c.partido.numero = :numero))
            """;

    /** Recorte por data de publicacao da noticia. */
    String FILTRO_PERIODO = """
              and c.dataPublicacao between :inicio and :fim
            """;

    // ------------------------------------------------------------ projecoes

    /** Volume e confianca media de um sentimento. */
    interface ContagemPorSentimento {
        String getSentimento();

        long getTotal();

        Double getScoreMedio();
    }

    /**
     * Volume de um sentimento dentro de um veiculo.
     * {@code idMidia} nulo agrupa as classificacoes sem veiculo associado.
     */
    interface ContagemPorMidia {
        Integer getIdMidia();

        String getNomeMidia();

        String getSentimento();

        long getTotal();
    }

    /** Quantas classificacoes cada modelo produziu. */
    interface ContagemPorModelo {
        String getMetodo();

        String getPipelineVersion();

        long getTotal();
    }

    // -------------------------------------------------------------- queries

    @Query("""
            select c.sentimento as sentimento,
                   count(c) as total,
                   avg(c.scoreConfianca) as scoreMedio
            from Classificacao c
            where 1 = 1
            """ + FILTRO_ENTIDADE + FILTRO_PERIODO + """
            group by c.sentimento
            """)
    List<ContagemPorSentimento> contarPorSentimento(@Param("titulo") String titulo,
                                                    @Param("numero") int numero,
                                                    @Param("inicio") LocalDateTime inicio,
                                                    @Param("fim") LocalDateTime fim);

    /**
     * O join e a esquerda de proposito: hoje a maioria das linhas nao tem
     * {@code id_midia}. Um join interno as descartaria em silencio e o agregado
     * apareceria menor que a cobertura real -- elas voltam com veiculo nulo,
     * para serem contadas como "nao identificado".
     */
    @Query("""
            select m.id as idMidia,
                   m.nome as nomeMidia,
                   c.sentimento as sentimento,
                   count(c) as total
            from Classificacao c
              left join c.midia m
            where 1 = 1
            """ + FILTRO_ENTIDADE + FILTRO_PERIODO + """
            group by m.id, m.nome, c.sentimento
            """)
    List<ContagemPorMidia> contarPorMidia(@Param("titulo") String titulo,
                                          @Param("numero") int numero,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim);

    /** {@code sentimento} vazio traz todos os sentimentos. */
    @Query("""
            select c from Classificacao c
              left join fetch c.midia
            where (:sentimento = '' or c.sentimento = :sentimento)
            """ + FILTRO_ENTIDADE + FILTRO_PERIODO + """
            order by c.dataPublicacao desc
            """)
    Page<Classificacao> buscarMencoes(@Param("titulo") String titulo,
                                      @Param("numero") int numero,
                                      @Param("sentimento") String sentimento,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim,
                                      Pageable pageable);

    @Query("""
            select c.metodo as metodo,
                   c.pipelineVersion as pipelineVersion,
                   count(c) as total
            from Classificacao c
            where 1 = 1
            """ + FILTRO_ENTIDADE + """
            group by c.metodo, c.pipelineVersion
            """)
    List<ContagemPorModelo> contarPorModelo(@Param("titulo") String titulo,
                                            @Param("numero") int numero);
}
