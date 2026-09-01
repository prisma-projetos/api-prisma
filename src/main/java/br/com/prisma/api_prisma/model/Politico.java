package br.com.prisma.api_prisma.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Politico, na granularidade da pessoa (nao da candidatura).
 *
 * <p>A chave primaria e o numero do titulo eleitoral: e o identificador
 * estavel do TSE, e o que amarra a pessoa as suas varias candidaturas ao
 * longo das eleicoes.
 */
@Entity
@Table(name = "tabela_politico")
public class Politico {

    @Id
    @Column(name = "nr_titulo_eleitoral_candidato", length = 20)
    private String tituloEleitoral;

    @Column(name = "nm_candidato")
    private String nome;

    @Column(name = "nm_social_candidato")
    private String nomeSocial;

    @Column(name = "dt_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "sg_uf_nascimento", columnDefinition = "bpchar(2)")
    private String ufNascimento;

    @Column(name = "ds_genero")
    private String genero;

    @Column(name = "ds_grau_instrucao")
    private String grauInstrucao;

    @Column(name = "ds_cor_raca")
    private String corRaca;

    @Column(name = "ds_ocupacao")
    private String ocupacao;

    @Column(name = "desc_politico", columnDefinition = "text")
    private String descricao;

    public String getTituloEleitoral() {
        return tituloEleitoral;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeSocial() {
        return nomeSocial;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getUfNascimento() {
        return ufNascimento;
    }

    public String getGenero() {
        return genero;
    }

    public String getGrauInstrucao() {
        return grauInstrucao;
    }

    public String getCorRaca() {
        return corRaca;
    }

    public String getOcupacao() {
        return ocupacao;
    }

    public String getDescricao() {
        return descricao;
    }
}
