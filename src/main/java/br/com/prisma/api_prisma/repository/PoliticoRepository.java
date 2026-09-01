package br.com.prisma.api_prisma.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.Politico;

public interface PoliticoRepository extends JpaRepository<Politico, String> {

    /**
     * Busca por nome ou nome social. O padrao ja chega pronto e minusculo do
     * servico: parametro nulo faria o Postgres inferir {@code bytea} e
     * {@code lower(bytea)} nao existe.
     */
    @Query("""
            select p from Politico p
            where lower(coalesce(p.nome, '')) like :pattern
               or lower(coalesce(p.nomeSocial, '')) like :pattern
            order by p.nome
            """)
    Page<Politico> buscarPorNome(@Param("pattern") String pattern, Pageable pageable);
}
