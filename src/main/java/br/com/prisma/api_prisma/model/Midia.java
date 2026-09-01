package br.com.prisma.api_prisma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Veiculo de imprensa de onde a noticia foi coletada. */
@Entity
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
