package br.com.prisma.api_prisma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

/**
 * Veiculo de imprensa de onde a noticia foi coletada.
 *
 * <p>Carregado em lote, e nao uma consulta por linha. A busca de materias em
 * {@code AnaliseItemRepository} e nativa, e consulta nativa nao tem
 * {@code join fetch} -- sem isto, cada veiculo distinto da pagina viraria uma
 * ida ao banco. Fica na classe alvo porque o Hibernate 7 nao aceita
 * {@code @BatchSize} sobre uma associacao para-um.
 */
@Entity
@BatchSize(size = 100)
@Table(name = "tabela_midia")
public class Midia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_midia")
    private Integer id;

    @Column(name = "nome_midia")
    private String nome;

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
