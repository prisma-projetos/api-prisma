package br.com.prisma.api_prisma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.Panorama;

/**
 * Consultas sobre os panoramas consolidados pelo {@code analyser-prisma}.
 *
 * <p>Mesma convencao de sentinela dos demais repositorios de analise.
 */
public interface PanoramaRepository extends JpaRepository<Panorama, Long> {

    /**
     * O predicado de entidade para o panorama consultado.
     *
     * <p>Vem em duas redacoes, uma por alias, porque a selecao por maior versao
     * precisa do mesmo predicado nas duas metades da consulta e o valor de uma
     * anotacao tem de ser constante de compilacao -- nao da para gerar o par a
     * partir de um molde. Ficam lado a lado de proposito: se divergirem, a
     * consulta devolve panorama de versao errada em silencio, e nao erro.
     */
    String FILTRO_ENTIDADE = """
              and ((:titulo <> '' and p.tituloEleitoral = :titulo)
                or (:numero <> -1 and p.nrPartido = :numero))
            """;

    /** Igual a {@link #FILTRO_ENTIDADE}, para o alias da subconsulta. */
    String FILTRO_ENTIDADE_SUBCONSULTA = """
              and ((:titulo <> '' and q.tituloEleitoral = :titulo)
                or (:numero <> -1 and q.nrPartido = :numero))
            """;

    /**
     * Os panoramas de uma janela, um por eixo, sempre na maior versao.
     *
     * <p>A subconsulta correlacionada e o que implementa "apenas a versao mais
     * recente": a tabela guarda todas as versoes e nunca sobrescreve. Filtrar
     * por {@code publicado_em} seria o criterio natural, mas o analyser nunca
     * escreve essa coluna -- ver a nota em {@link Panorama}.
     */
    @Query("""
            select p from Panorama p
            where p.janela = :janela
            """ + FILTRO_ENTIDADE + """
              and p.versao = (select max(q.versao) from Panorama q
                               where q.janela = p.janela
                                 and q.eixo = p.eixo
                                 and q.tipoEntidade = p.tipoEntidade
            """ + FILTRO_ENTIDADE_SUBCONSULTA + """
                              )
            order by p.eixo asc
            """)
    List<Panorama> buscarMaisRecentes(@Param("titulo") String titulo,
                                      @Param("numero") int numero,
                                      @Param("janela") String janela);

    /** Janela mais recente com panorama para a entidade, ou null se nao houver. */
    @Query("""
            select max(p.janela) from Panorama p
            where 1 = 1
            """ + FILTRO_ENTIDADE)
    String janelaMaisRecente(@Param("titulo") String titulo, @Param("numero") int numero);
}
