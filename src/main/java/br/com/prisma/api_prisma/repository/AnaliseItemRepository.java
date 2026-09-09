package br.com.prisma.api_prisma.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.AnaliseItem;

/**
 * Consultas sobre as materias analisadas pelo {@code analyser-prisma}.
 *
 * <p>Segue o mesmo contrato de {@link ClassificacaoRepository}: o alvo entra
 * por sentinela, e nao por null. O Postgres infere {@code bytea} para um NULL
 * sem tipo e a comparacao falha em tempo de execucao -- entao "sem filtro"
 * viaja como valor concreto ({@code ""}, {@code -1} e, para datas, os limites
 * abertos), normalizado no servico.
 *
 * <p>Diferente da classificacao, aqui o filtro cai direto em colunas da
 * propria tabela: o analyser nao tem FK para politico nem partido. Isso
 * dispensa o cuidado com o join implicito descrito em
 * {@link ClassificacaoRepository} -- nao ha associacao a navegar.
 */
public interface AnaliseItemRepository extends JpaRepository<AnaliseItem, String> {

    String FILTRO_ENTIDADE = """
              and ((:titulo <> '' and i.tituloEleitoral = :titulo)
                or (:numero <> -1 and i.nrPartido = :numero))
            """;

    /**
     * Recorte por data de publicacao.
     *
     * <p>{@code data_publicacao} e anulavel, e {@code between} descarta nulo.
     * Uma materia sem data sumiria em silencio de todo agregado -- o mesmo
     * defeito que o left join por midia evita do outro lado da API. O
     * {@code coalesce} para o limite aberto resolve isso dentro da propria
     * regra, sem parametro extra: com periodo em aberto {@code :inicio} <i>e</i>
     * o limite aberto, entao a materia sem data cai dentro; com periodo
     * pedido, {@code :inicio} e posterior e ela fica de fora.
     */
    String FILTRO_PERIODO = """
              and coalesce(i.dataPublicacao, :aberto) between :inicio and :fim
            """;

    /** Quantas materias cada par (modelo, regra) produziu. */
    interface ContagemPorModelo {
        String getModelo();

        String getRegraAgregacao();

        long getTotal();
    }

    /**
     * Recorte das materias de uma entidade, em SQL nativo.
     *
     * <p>Nativo por causa de um filtro so: os temas vivem num array JSON e
     * {@code @>} nao tem grafia em JPQL. O preco esta assumido e e duplo -- a
     * regra de entidade e de periodo aparece aqui numa segunda redacao, e esta
     * consulta nao passa pela validacao de contexto que cobre as JPQL vizinhas
     * (ver {@code AnaliseItemRepositoryTest}). Mexeu em {@link #FILTRO_ENTIDADE}
     * ou {@link #FILTRO_PERIODO}? Mexa aqui tambem.
     *
     * <p>Todo parametro vai com {@code cast} explicito porque o Postgres nao
     * infere tipo de parametro solto num {@code =} contra literal, e o erro
     * aparece so em tempo de execucao.
     */
    String SELECAO_MATERIAS = """
            from analyser_item i
            where (cast(:tema as text) = ''
                   or i.temas_json @> to_jsonb(cast(:tema as text)))
              and (cast(:janela as text) = '' or i.janela_mes = cast(:janela as text))
              and ((cast(:titulo as text) <> ''
                    and i.nr_titulo_eleitoral_candidato = cast(:titulo as text))
                or (cast(:numero as integer) <> -1
                    and i.nr_partido = cast(:numero as integer)))
              and coalesce(i.data_publicacao, :aberto) between :inicio and :fim
            """;

    /**
     * Materias da entidade, da mais recente para a mais antiga.
     *
     * <p>Sem {@code join fetch} -- consulta nativa nao tem. A midia carrega em
     * lote, pelo {@code @BatchSize} declarado em {@link AnaliseItem}.
     */
    @Query(value = "select i.* " + SELECAO_MATERIAS
            + " order by i.data_publicacao desc, i.item_id asc",
            countQuery = "select count(*) " + SELECAO_MATERIAS,
            nativeQuery = true)
    Page<AnaliseItem> buscarMaterias(@Param("titulo") String titulo,
                                     @Param("numero") int numero,
                                     @Param("tema") String tema,
                                     @Param("janela") String janela,
                                     @Param("inicio") OffsetDateTime inicio,
                                     @Param("fim") OffsetDateTime fim,
                                     @Param("aberto") OffsetDateTime aberto,
                                     Pageable pageable);

    /**
     * So a coluna de temas, de todas as materias do periodo.
     *
     * <p>Os temas vivem num array JSON, entao {@code group by} nao alcanca --
     * a contagem acontece no servico, como ja faz {@code AcumuladorMidia} em
     * {@code AnaliseQueryService}. Projetar a coluna, e nao a entidade, evita
     * trazer o {@code resumo} de cada materia para conta-las: e o unico campo
     * que o agregado le.
     */
    @Query("""
            select i.temasJson from AnaliseItem i
            where 1 = 1
            """ + FILTRO_ENTIDADE + FILTRO_PERIODO)
    List<String> listarTemasParaAgregacao(@Param("titulo") String titulo,
                                          @Param("numero") int numero,
                                          @Param("inicio") OffsetDateTime inicio,
                                          @Param("fim") OffsetDateTime fim,
                                          @Param("aberto") OffsetDateTime aberto);

    @Query("""
            select i.modelo as modelo,
                   i.regraAgregacao as regraAgregacao,
                   count(i) as total
            from AnaliseItem i
            where 1 = 1
            """ + FILTRO_ENTIDADE + """
            group by i.modelo, i.regraAgregacao
            """)
    List<ContagemPorModelo> contarPorModelo(@Param("titulo") String titulo,
                                            @Param("numero") int numero);

    /** Resolve ids de fonte do panorama para materia navegavel. */
    @Query("""
            select i from AnaliseItem i
              left join fetch i.midia
            where i.id in :ids
            """)
    List<AnaliseItem> buscarPorIds(@Param("ids") Collection<String> ids);
}
