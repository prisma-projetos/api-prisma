package br.com.prisma.api_prisma.repository;

import java.util.List;

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
}
