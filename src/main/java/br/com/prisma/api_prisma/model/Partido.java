package br.com.prisma.api_prisma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Partido politico, identificado pelo numero de legenda do TSE. */
@Entity
@Table(name = "tabela_partido")
public class Partido {

    @Id
    @Column(name = "nr_partido")
    private Integer numero;

    @Column(name = "sg_partido")
    private String sigla;

    @Column(name = "nm_partido")
    private String nome;

    @Column(name = "desc_partido", columnDefinition = "text")
    private String descricao;

    public Integer getNumero() {
        return numero;
    }

    public String getSigla() {
        return sigla;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }
}
