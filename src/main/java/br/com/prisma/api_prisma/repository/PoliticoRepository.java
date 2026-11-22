package br.com.prisma.api_prisma.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prisma.api_prisma.model.Politico;

public interface PoliticoRepository extends JpaRepository<Politico, String> {
}
