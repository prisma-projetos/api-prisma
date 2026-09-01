package br.com.prisma.api_prisma.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pleito eleitoral (ano, tipo e turno). */
@Entity
@Table(name = "tabela_eleicao")
public class Eleicao {

    @Id
    @Column(name = "cd_eleicao")
    private Integer codigo;

    @Column(name = "ano_eleicao")
    private Integer ano;

    @Column(name = "nm_tipo_eleicao")
    private String tipo;

    @Column(name = "nr_turno")
    private Integer turno;

    @Column(name = "ds_eleicao")
    private String descricao;

    @Column(name = "dt_eleicao")
    private LocalDate data;

    @Column(name = "tp_abrangencia")
    private String abrangencia;

    public Integer getCodigo() {
        return codigo;
    }

    public Integer getAno() {
        return ano;
    }

    public String getTipo() {
        return tipo;
    }

    public Integer getTurno() {
        return turno;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getData() {
        return data;
    }

    public String getAbrangencia() {
        return abrangencia;
    }
}
