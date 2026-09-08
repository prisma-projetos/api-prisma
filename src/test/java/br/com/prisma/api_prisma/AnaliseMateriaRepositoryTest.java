package br.com.prisma.api_prisma;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import br.com.prisma.api_prisma.repository.AnaliseMateriaRepository;

/**
 * Valida o que os testes de controller nao alcancam: se a entidade
 * {@code AnaliseMateria} mapeia contra o resto do modelo, e se as queries
 * JPQL do repositorio compilam.
 *
 * <p><b>O que este teste cobre.</b> O Spring Data monta e valida toda
 * {@code @Query} declarada ao criar o bean do repositorio, e o Hibernate
 * resolve o modelo de entidades ao construir o {@code EntityManagerFactory}.
 * Um campo inexistente, um alias errado ou um JPQL malformado derrubam o
 * contexto -- e portanto este teste -- antes de qualquer assercao. Hoje um
 * typo em {@code @Query} so apareceria na primeira chamada em producao.
 *
 * <p><b>O que este teste NAO cobre.</b> Nao ha schema: {@code ddl-auto=none}
 * porque o H2 nao conhece {@code bpchar}, tipo do Postgres usado em
 * {@code Politico.ufNascimento}. Entao isto nao prova que as queries
 * <i>executam</i> nem que o mapeamento bate com o banco real -- so o profile
 * {@code dev}, que roda {@code validate} contra o Postgres, prova isso.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:prisma;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AnaliseMateriaRepositoryTest {

    @Autowired
    private AnaliseMateriaRepository analises;

    /**
     * Chegar aqui ja significa que o modelo resolveu e que as tres queries
     * declaradas em {@link AnaliseMateriaRepository} foram aceitas.
     */
    @Test
    void modeloEQueriesSaoValidos() {
        assertThat(analises).isNotNull();
    }
}
