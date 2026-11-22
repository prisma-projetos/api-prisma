package br.com.prisma.api_prisma.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.Candidatura;

public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {

    /** Candidaturas de um politico, da mais recente para a mais antiga. */
    @Query("""
            select c from Candidatura c
              left join fetch c.partido
              left join fetch c.eleicao e
            where c.politico.tituloEleitoral = :titulo
            order by e.ano desc, e.turno desc
            """)
    List<Candidatura> doPolitico(@Param("titulo") String titulo);

    /**
     * Lista candidaturas por nome/nome social do politico, com filtros opcionais
     * de partido, ano de eleicao e cargo. {@code nrPartido}/{@code anoEleicao}
     * nulos casam com tudo -- sem o ano, um politico com candidaturas em mais
     * de uma eleicao aparece uma vez por eleicao. {@code cargo} e comparado
     * sem diferenciar maiusculas/minusculas, como o resto da busca. O partido
     * vem no mesmo select (fetch join) porque a listagem expoe numero e sigla
     * -- sem ele, seria uma consulta por item.
     */
    @Query("""
            select c from Candidatura c
              left join fetch c.partido
              left join fetch c.eleicao e
              left join fetch c.politico p
            where (lower(coalesce(p.nome, '')) like :pattern
               or lower(coalesce(p.nomeSocial, '')) like :pattern)
              and (:nrPartido is null or c.partido.numero = :nrPartido)
              and (:anoEleicao is null or e.ano = :anoEleicao)
              and (:cargo is null or lower(c.cargo) = :cargo)
              and (:uf is null or c.uf = :uf)
            order by p.nome
            """)
    Page<Candidatura> buscar(@Param("pattern") String pattern,
                              @Param("nrPartido") Integer nrPartido,
                              @Param("anoEleicao") Integer anoEleicao,
                              @Param("cargo") String cargo,
                              @Param("uf") String uf,
                              Pageable pageable);
}
