package br.com.prisma.api_prisma.model;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Indice de nomes usado para reconhecer entidades no texto das noticias.
 *
 * <p>Cada linha aponta para um politico OU um partido -- nunca os dois, o que
 * e garantido por CHECK no banco. Por isso as referencias sao guardadas como
 * chaves cruas ({@code tituloEleitoral}, {@code nrPartido}) e nao como
 * associacoes JPA: uma das duas e sempre nula.
 */
@Entity
@Table(name = "entidade_matching")
public class EntidadeMatching {

    public static final String TIPO_POLITICO = "politico";
    public static final String TIPO_PARTIDO = "partido";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, columnDefinition = "text")
    private String tipo;

    @Column(name = "nr_titulo_eleitoral_candidato", columnDefinition = "text")
    private String tituloEleitoral;

    @Column(name = "nr_partido")
    private Integer nrPartido;

    @Column(name = "nome_principal", nullable = false, columnDefinition = "text")
    private String nomePrincipal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases", nullable = false)
    private List<String> aliases;

    @Column(name = "sg_uf", columnDefinition = "text")
    private String uf;

    @Column(name = "ds_cargo", columnDefinition = "text")
    private String cargo;

    /** De onde veio o registro (ex.: carga do TSE, curadoria manual). */
    @Column(name = "origem", nullable = false, columnDefinition = "text")
    private String origem;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTituloEleitoral() {
        return tituloEleitoral;
    }

    public Integer getNrPartido() {
        return nrPartido;
    }

    public String getNomePrincipal() {
        return nomePrincipal;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getUf() {
        return uf;
    }

    public String getCargo() {
        return cargo;
    }

    public String getOrigem() {
        return origem;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
