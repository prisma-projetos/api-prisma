package br.com.prisma.api_prisma.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Resultado da analise de um paragrafo de noticia em relacao a uma entidade.
 *
 * <p>A unidade analisada e o <b>paragrafo</b>, nao o artigo: uma mesma noticia
 * gera varias linhas, e um paragrafo que cita duas entidades gera uma linha
 * para cada. Por construcao, cada linha aponta para um politico OU um partido.
 *
 * <p>{@code idArtigoScrape} referencia o artigo no banco do scraper, que e
 * externo a este banco -- por isso nao ha chave estrangeira nem entidade JPA
 * para ele. Para chegar a noticia, use {@code linkNoticia}.
 *
 * <p>A unicidade e por
 * (artigo, paragrafo, politico, partido, {@code pipelineVersion}): reprocessar
 * com a mesma versao do pipeline nao duplica, e uma versao nova convive com a
 * anterior em vez de sobrescreve-la.
 */
@Entity
@Table(name = "tabela_classificacao")
public class Classificacao {

    /** Valores observados em classificacao_sentimento. */
    public static final String SENTIMENTO_POSITIVO = "positivo";
    public static final String SENTIMENTO_NEUTRO = "neutro";
    public static final String SENTIMENTO_NEGATIVO = "negativo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_classificacao")
    private Integer id;

    @Column(name = "id_artigo_scrape")
    private Long idArtigoScrape;

    @Column(name = "paragrafo_analisado", columnDefinition = "text")
    private String paragrafo;

    @Column(name = "link_noticia", columnDefinition = "text")
    private String linkNoticia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nr_titulo_eleitoral_candidato")
    private Politico politico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nr_partido")
    private Partido partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_midia")
    private Midia midia;

    @Column(name = "classificacao_sentimento", length = 50)
    private String sentimento;

    @Column(name = "score_confianca")
    private Double scoreConfianca;

    @Column(name = "data_publicacao_noticia")
    private LocalDateTime dataPublicacao;

    @Column(name = "registro_classificacao")
    private LocalDateTime registradoEm;

    @Column(name = "metodo_classificacao", length = 25)
    private String metodo;

    @Column(name = "pipeline_version", nullable = false, columnDefinition = "text")
    private String pipelineVersion;

    /** Como a entidade foi reconhecida no texto -- base para auditar o match. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidencia_match")
    private Map<String, Object> evidenciaMatch;

    public Integer getId() {
        return id;
    }

    public Long getIdArtigoScrape() {
        return idArtigoScrape;
    }

    public String getParagrafo() {
        return paragrafo;
    }

    public String getLinkNoticia() {
        return linkNoticia;
    }

    public Politico getPolitico() {
        return politico;
    }

    public Partido getPartido() {
        return partido;
    }

    public Midia getMidia() {
        return midia;
    }

    public String getSentimento() {
        return sentimento;
    }

    public Double getScoreConfianca() {
        return scoreConfianca;
    }

    public LocalDateTime getDataPublicacao() {
        return dataPublicacao;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public String getMetodo() {
        return metodo;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public Map<String, Object> getEvidenciaMatch() {
        return evidenciaMatch;
    }
}
