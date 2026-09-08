package br.com.prisma.api_prisma.model;

import java.time.LocalDateTime;

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
 * Analise de uma materia inteira em relacao a uma entidade politica.
 *
 * <p>Nao confundir com {@link Classificacao}: aquela e por <b>paragrafo</b> e
 * vem do {@code classifier-prisma}; esta e por <b>materia</b> e vem do
 * {@code analyser-prisma}. As duas convivem, e nenhuma depende da outra --
 * por isso nao ha chave estrangeira entre elas. Para cruzar, o caminho e
 * {@code (idArtigoScrape, entidade)}.
 *
 * <p>Como na classificacao, cada linha aponta para um politico OU um partido,
 * e a unicidade inclui a {@code pipelineVersion}: reprocessar com a mesma
 * versao nao duplica, e uma versao nova convive com a anterior.
 *
 * <p><b>Schema ainda nao confirmado.</b> Os campos abaixo refletem o que foi
 * descrito para o output do analyser (eixo, titulo, resumo, entidade) e as
 * convencoes ja usadas em {@code tabela_classificacao}. Enquanto o
 * {@code analyser-prisma} nao criar a tabela real, isto so sobe no profile
 * {@code local}, onde o Hibernate a gera a partir desta classe.
 */
@Entity
@Table(name = "tabela_analise")
public class AnaliseMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analise")
    private Integer id;

    @Column(name = "id_artigo_scrape")
    private Long idArtigoScrape;

    @Column(name = "link_noticia", columnDefinition = "text")
    private String linkNoticia;

    @Column(name = "data_publicacao_noticia")
    private LocalDateTime dataPublicacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_midia")
    private Midia midia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nr_titulo_eleitoral_candidato")
    private Politico politico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nr_partido")
    private Partido partido;

    /** Recorte tematico da materia. Vocabulario ainda em aberto. */
    @Column(name = "eixo", length = 50)
    private String eixo;

    @Column(name = "titulo", columnDefinition = "text")
    private String titulo;

    @Column(name = "resumo", columnDefinition = "text")
    private String resumo;

    /**
     * Metodo que produziu a analise. Tratamos titulo e resumo como saida de
     * modelo ate que se prove o contrario: se forem extraidos da materia, este
     * campo e inofensivo; se forem gerados e nao existisse, o resumo circularia
     * sem dizer quem o escreveu.
     */
    @Column(name = "metodo_analise", length = 25)
    private String metodo;

    @Column(name = "pipeline_version", nullable = false, columnDefinition = "text")
    private String pipelineVersion;

    @Column(name = "registro_analise")
    private LocalDateTime registradoEm;

    public Integer getId() {
        return id;
    }

    public Long getIdArtigoScrape() {
        return idArtigoScrape;
    }

    public String getLinkNoticia() {
        return linkNoticia;
    }

    public LocalDateTime getDataPublicacao() {
        return dataPublicacao;
    }

    public Midia getMidia() {
        return midia;
    }

    public Politico getPolitico() {
        return politico;
    }

    public Partido getPartido() {
        return partido;
    }

    public String getEixo() {
        return eixo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getResumo() {
        return resumo;
    }

    public String getMetodo() {
        return metodo;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }
}
