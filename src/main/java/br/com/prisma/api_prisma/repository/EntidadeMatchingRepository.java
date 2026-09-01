package br.com.prisma.api_prisma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.EntidadeMatching;

public interface EntidadeMatchingRepository extends JpaRepository<EntidadeMatching, Long> {

    /**
     * Entidades ativas cujo nome principal casa com o termo. Os aliases ficam
     * de fora da busca porque a coluna e JSONB: percorre-la exigiria SQL
     * nativo especifico do Postgres.
     */
    @Query("""
            select e from EntidadeMatching e
            where e.ativo = true
              and lower(e.nomePrincipal) like :pattern
            order by e.nomePrincipal
            """)
    List<EntidadeMatching> buscarAtivas(@Param("pattern") String pattern);
}
