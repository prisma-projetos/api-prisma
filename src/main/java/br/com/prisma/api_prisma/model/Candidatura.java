package br.com.prisma.api_prisma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Candidatura de um politico em uma eleicao.
 *
 * <p>E aqui que vivem cargo, UF, partido e resultado -- um mesmo politico tem
 * varias, e o partido pode mudar entre elas. Por isso a filiacao nao esta em
 * {@link Politico}: ela so faz sentido dentro de uma candidatura.
 */
@Entity
@Table(name = "tabela_candidatura")
public class Candidatura {

    @Id
    @Column(name = "sq_candidato")
    private Long sequencial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nr_titulo_eleitoral_candidato")
    private Politico politico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cd_eleicao")
    private Eleicao eleicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nr_partido")
    private Partido partido;

    @Column(name = "ds_cargo")
    private String cargo;

    @Column(name = "nr_candidato")
    private Integer numeroUrna;

    @Column(name = "nm_urna_candidato")
    private String nomeUrna;

    @Column(name = "sg_uf", columnDefinition = "bpchar(2)")
    private String uf;

    @Column(name = "nm_ue")
    private String unidadeEleitoral;

    @Column(name = "ds_situacao_candidatura")
    private String situacaoCandidatura;

    @Column(name = "ds_sit_tot_turno")
    private String situacaoTurno;

    @Column(name = "nm_coligacao")
    private String coligacao;

    @Column(name = "sg_federacao")
    private String federacao;

    public Long getSequencial() {
        return sequencial;
    }

    public Politico getPolitico() {
        return politico;
    }

    public Eleicao getEleicao() {
        return eleicao;
    }

    public Partido getPartido() {
        return partido;
    }

    public String getCargo() {
        return cargo;
    }

    public Integer getNumeroUrna() {
        return numeroUrna;
    }

    public String getNomeUrna() {
        return nomeUrna;
    }

    public String getUf() {
        return uf;
    }

    public String getUnidadeEleitoral() {
        return unidadeEleitoral;
    }

    public String getSituacaoCandidatura() {
        return situacaoCandidatura;
    }

    public String getSituacaoTurno() {
        return situacaoTurno;
    }

    public String getColigacao() {
        return coligacao;
    }

    public String getFederacao() {
        return federacao;
    }
}
