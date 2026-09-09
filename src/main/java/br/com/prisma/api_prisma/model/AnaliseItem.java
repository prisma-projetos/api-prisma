package br.com.prisma.api_prisma.model;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma materia analisada em relacao a uma entidade politica -- uma linha por
 * artigo x ator, escrita pelo {@code analyser-prisma}.
 *
 * <p>Nao confundir com {@link Classificacao}: aquela e por <b>paragrafo</b> e
 * vem do {@code classifier-prisma}; esta e por <b>materia</b>. As duas
 * convivem e nenhuma depende da outra -- nao ha chave estrangeira entre elas.
 * Para cruzar, o caminho e {@code (articleId, entidade)}.
 *
 * <p><b>Nao existe titulo.</b> O analyser recebe um titulo no resumo, mas nao
 * o persiste: a lista de colunas do {@code INSERT} em {@code gravar_resumo}
 * nao o inclui. Por isso nenhuma resposta desta API promete titulo de materia
 * -- prometer um campo que o banco nao guarda seria devolver null para sempre.
 *
 * <p><b>Nao ha FK para politico ou partido.</b> Diferente de
 * {@code tabela_classificacao}, o analyser grava os identificadores como
 * colunas soltas. Mapeamos igual, como colunas -- o que de quebra dispensa o
 * cuidado com join implicito que {@code AnaliseItemRepository} descreve.
 */
@Entity
@Table(name = "analyser_item")
public class AnaliseItem {

    /** {@code art_1__politico_122418060191}. Tambem e o id de fonte no panorama. */
    @Id
    @Column(name = "item_id")
    private String id;

    @Column(name = "article_id")
    private Long articleId;

    /** {@code politico} ou {@code partido}. */
    @Column(name = "entity_type", length = 20)
    private String tipoEntidade;

    @Column(name = "nr_titulo_eleitoral_candidato", length = 20)
    private String tituloEleitoral;

    @Column(name = "nr_partido")
    private Integer nrPartido;

    /**
     * Retrato do nome no momento da gravacao. Para o nome corrente, o caminho
     * e {@code entidade_matching} -- ver comentario da coluna na migration do
     * analyser.
     */
    @Column(name = "ator_nome", length = 255)
    private String atorNome;

    /** Carrega em lote -- ver o {@code @BatchSize} em {@link Midia}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_midia")
    private Midia midia;

    @Column(name = "link_noticia", columnDefinition = "text")
    private String linkNoticia;

    @Column(name = "data_publicacao")
    private OffsetDateTime dataPublicacao;

    /** {@code YYYY-MM}, derivado da data da materia. E o agrupamento do panorama. */
    // CHAR(7), nao VARCHAR: 'YYYY-MM' tem exatamente 7 caracteres, entao o
    // padding do bpchar nunca aparece -- mas o validate compara o tipo JDBC e
    // recusa a subida se o mapeamento disser varchar.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "janela_mes", columnDefinition = "bpchar(7)")
    private String janela;

    /**
     * Sentimento da materia inteira: {@code positivo}, {@code negativo} ou
     * {@code neutro}. E o que o analyser chama de <b>eixo</b> -- ver a nota de
     * vocabulario em {@link Panorama}.
     */
    @Column(name = "rotulo_definitivo", length = 10)
    private String rotulo;

    /** Regra que produziu o rotulo (ex.: {@code eixo-por-sinal-v2}). */
    @Column(name = "regra_agregacao_versao", length = 30)
    private String regraAgregacao;

    @Column(name = "resumo", columnDefinition = "text")
    private String resumo;

    /**
     * Array JSON de strings -- {@code ["saude", "educacao"]}.
     *
     * <p><b>Formato diferente do {@code temas_json} do panorama</b>, que e uma
     * lista de objetos com contagem. Aqui o analyser grava
     * {@code Jsonb(registro.temas)} sobre um {@code list[str]}. Guardamos o
     * texto cru e desserializamos no servico, para que um item com JSON fora
     * do previsto vire lista vazia em vez de derrubar a consulta inteira.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "temas_json", columnDefinition = "jsonb")
    private String temasJson;

    /** Modelo que escreveu o resumo. Nulo enquanto o item nao foi resumido. */
    @Column(name = "modelo", length = 100)
    private String modelo;

    /** {@code novo}, {@code resumido} ou {@code consolidado}. */
    @Column(name = "status", length = 20)
    private String status;

    public String getId() {
        return id;
    }

    public Long getArticleId() {
        return articleId;
    }





    public Midia getMidia() {
        return midia;
    }

    public String getLinkNoticia() {
        return linkNoticia;
    }

    public OffsetDateTime getDataPublicacao() {
        return dataPublicacao;
    }

    public String getJanela() {
        return janela;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getRegraAgregacao() {
        return regraAgregacao;
    }

    public String getResumo() {
        return resumo;
    }

    public String getTemasJson() {
        return temasJson;
    }

    public String getModelo() {
        return modelo;
    }

    public String getStatus() {
        return status;
    }
}
