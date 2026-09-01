package br.com.prisma.api_prisma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.prisma.api_prisma.model.Partido;

public interface PartidoRepository extends JpaRepository<Partido, Integer> {

    @Query("""
            select p from Partido p
            where lower(coalesce(p.sigla, '')) like :pattern
               or lower(coalesce(p.nome, '')) like :pattern
            order by p.sigla
            """)
    List<Partido> buscar(@Param("pattern") String pattern);
}
