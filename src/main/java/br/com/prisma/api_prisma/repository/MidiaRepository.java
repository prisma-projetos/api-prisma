package br.com.prisma.api_prisma.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prisma.api_prisma.model.Midia;

public interface MidiaRepository extends JpaRepository<Midia, Integer> {
}
