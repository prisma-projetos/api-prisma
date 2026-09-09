package br.com.prisma.api_prisma.model;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Texto consolidado sobre uma entidade numa janela mensal, por eixo.
 *
 * <p><b>Nota de vocabulario -- leia antes de mexer.</b> No analyser,
 * {@code eixo} e <b>polaridade de sentimento</b>: o proprio banco restringe a
 * coluna a {@code positivo}, {@code negativo} e {@code neutro}. Nao e recorte
 * tematico. O recorte tematico existe e se chama <b>tema</b>, e vive em
 * {@code temas_json}. Confundir os dois faria a API agregar sentimento e
 * rotular como pauta.
 *
 * <p><b>Versionado e nunca sobrescrito.</b> Cada recalculo insere uma versao
 * nova e as anteriores permanecem para auditoria. A API expoe apenas a maior
 * versao de cada {@code (entidade, janela, eixo)}.
 *
 * <p><b>Por que a selecao nao usa {@code publicado_em}.</b> A coluna existe na
 * migration, mas o analyser nunca a escreve -- nao esta na lista de colunas do
 * {@code INSERT} em {@code gravar_panorama}. Filtrar por
 * {@code publicado_em is not null} devolveria vazio para sempre, e vazio em
 * silencio e o pior tipo de bug aqui. Enquanto o analyser nao publicar de
 * fato, a maior versao e a melhor definicao disponivel de "mais recente" --
 * que e tambem a que o proprio analyser usa em {@code panoramas_mais_recentes}.
 */
@Entity
@Table(name = "analyser_panorama")
public class Panorama {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "panorama_id")
    private Long id;

    @Column(name = "entity_type", length = 20)
    private String tipoEntidade;

    @Column(name = "nr_titulo_eleitoral_candidato", length = 20)
    private String tituloEleitoral;

    @Column(name = "nr_partido")
    private Integer nrPartido;

    @Column(name = "ator_nome", length = 255)
    private String atorNome;

    /** {@code YYYY-MM}. */
    // CHAR(7), nao VARCHAR: 'YYYY-MM' tem exatamente 7 caracteres, entao o
    // padding do bpchar nunca aparece -- mas o validate compara o tipo JDBC e
    // recusa a subida se o mapeamento disser varchar.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "janela_mes", columnDefinition = "bpchar(7)")
    private String janela;

    /** {@code positivo}, {@code negativo} ou {@code neutro} -- ver nota da classe. */
    @Column(name = "eixo", length = 10)
    private String eixo;

    @Column(name = "versao")
    private Integer versao;

    @Column(name = "total_itens")
    private Integer totalItens;

    /** O paragrafo consolidado. E o conteudo que o cidadao le, e nunca e vazio. */
    @Column(name = "texto", columnDefinition = "text")
    private String texto;

    /**
     * Lista JSON de objetos {@code {id, tema, ocorrencias, fontes[]}}.
     *
     * <p>Formato diferente do {@code temas_json} de {@link AnaliseItem}, que e
     * um array de strings. Sao duas colunas homonimas com contratos distintos.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "temas_json", columnDefinition = "jsonb")
    private String temasJson;

    /** Array JSON com os {@code item_id} que sustentam o panorama. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fontes_json", columnDefinition = "jsonb")
    private String fontesJson;

    /** Divergencias que a verificacao registrou sem corrigir. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avisos_json", columnDefinition = "jsonb")
    private String avisosJson;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "criado_em")
    private OffsetDateTime criadoEm;







    public String getEixo() {
        return eixo;
    }

    public Integer getVersao() {
        return versao;
    }

    public Integer getTotalItens() {
        return totalItens;
    }

    public String getTexto() {
        return texto;
    }

    public String getTemasJson() {
        return temasJson;
    }

    public String getFontesJson() {
        return fontesJson;
    }

    public String getAvisosJson() {
        return avisosJson;
    }

    public String getModelo() {
        return modelo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
