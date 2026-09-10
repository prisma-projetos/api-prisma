package br.com.prisma.api_prisma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.model.AnaliseItem;
import br.com.prisma.api_prisma.model.Midia;
import br.com.prisma.api_prisma.model.Panorama;
import br.com.prisma.api_prisma.model.Partido;
import br.com.prisma.api_prisma.repository.AnaliseItemRepository;
import br.com.prisma.api_prisma.repository.PanoramaRepository;

/**
 * A agregacao por tema e a montagem do panorama sao a logica de verdade deste
 * servico: ordenacao, percentual, tratamento de tema ausente e resolucao dos
 * ids de fonte. O teste de controller mocka o servico inteiro, entao nada
 * disso seria exercitado.
 *
 * <p>Os temas chegam do banco como JSON cru, entao os cenarios abaixo passam
 * JSON de verdade -- e nao objetos ja desserializados. E onde mora o risco:
 * {@code analyser_item.temas_json} e um array de strings e
 * {@code analyser_panorama.temas_json} e um array de objetos, duas colunas
 * homonimas com formatos diferentes.
 */
class AnaliseMateriaQueryServiceTest {

    private AnaliseItemRepository itens;
    private PanoramaRepository panoramas;
    private AnaliseMateriaQueryService service;

    /** Consulta por partido, sem recorte de periodo, tema ou janela. */
    private static final ConsultaRequest POR_PARTIDO =
            new ConsultaRequest(null, 13, null, null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        var catalogo = mock(CatalogoQueryService.class);
        var partido = mock(Partido.class);
        when(partido.getNumero()).thenReturn(13);
        when(partido.getSigla()).thenReturn("PT");
        when(catalogo.carregarPartido(anyInt())).thenReturn(partido);

        itens = mock(AnaliseItemRepository.class);
        when(itens.contarPorModelo(anyString(), anyInt())).thenReturn(List.of());

        panoramas = mock(PanoramaRepository.class);

        service = new AnaliseMateriaQueryService(catalogo, itens, panoramas,
                new ObjectMapper());
    }

    // ------------------------------------------------------------ por tema

    @Test
    void ordenaTemasPorVolumeECalculaPercentual() {
        materias(temas("saude"), temas("saude"), temas("saude"),
                temas("educacao"), temas("seguranca"));

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isEqualTo(5);
        assertThat(resposta.semTema()).isZero();
        assertThat(resposta.temas()).extracting("tema")
                .containsExactly("saude", "educacao", "seguranca");
        assertThat(resposta.temas().get(0).total()).isEqualTo(3);
        assertThat(resposta.temas().get(0).percentual()).isEqualTo(60.0);
    }

    /**
     * Uma materia com dois temas conta nos dois -- e por isso a soma dos
     * totais passa do total de materias. O percentual continua sobre materias,
     * para responder "que fatia da cobertura toca este tema".
     */
    @Test
    void materiaComVariosTemasContaEmCadaUm() {
        materias(temas("saude", "economia"), temas("saude"));

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isEqualTo(2);
        assertThat(resposta.temas()).extracting("tema", "total")
                .containsExactly(tuple("saude", 2L), tuple("economia", 1L));
        assertThat(resposta.temas().get(0).percentual()).isEqualTo(100.0);
        assertThat(resposta.temas().get(1).percentual()).isEqualTo(50.0);
    }

    /** Tema repetido dentro da mesma materia e uma materia, nao duas. */
    @Test
    void temaRepetidoNaMesmaMateriaContaUmaVez() {
        materias(temas("saude", "saude"));

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.temas()).singleElement()
                .satisfies(t -> assertThat(t.total()).isEqualTo(1));
    }

    /** Materia sem tema e informacao, nao lixo: vira grupo proprio, no fim. */
    @Test
    void materiasSemTemaViramGrupoProprioNoFim() {
        materias(temas("saude"), temas("saude"), temas("saude"),
                semTemas(), semTemas());

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isEqualTo(5);
        assertThat(resposta.semTema()).isEqualTo(2);
        assertThat(resposta.temas()).extracting("tema")
                .containsExactly("saude", "(tema nao atribuido)");
        assertThat(resposta.temas().get(1).percentual()).isEqualTo(40.0);
    }

    /**
     * JSON gravado por outro componente e entrada externa. Um formato
     * inesperado tira aquela materia do recorte de pauta -- nunca derruba a
     * resposta inteira.
     */
    @Test
    void temaComJsonIlegivelNaoDerrubaAConsulta() {
        materias("isto nao e json", temas("saude"));

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isEqualTo(2);
        assertThat(resposta.semTema()).isEqualTo(1);
        assertThat(resposta.temas()).extracting("tema")
                .containsExactly("saude", "(tema nao atribuido)");
    }

    /** Entidade sem nenhuma materia nao pode estourar divisao por zero. */
    @Test
    void semMateriasDevolveAgregadoVazio() {
        materias();

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isZero();
        assertThat(resposta.semTema()).isZero();
        assertThat(resposta.temas()).isEmpty();
        assertThat(resposta.tipo()).isEqualTo("partido");
        assertThat(resposta.nome()).isEqualTo("PT");
    }

    /** Quando tudo cai num tema so, o numero tem que dizer isso: 100%. */
    @Test
    void concentracaoTotalNumTemaApareceComoCemPorCento() {
        materias(temas("saude"), temas("saude"));

        var resposta = service.porTema(POR_PARTIDO);

        assertThat(resposta.temas()).singleElement()
                .satisfies(t -> assertThat(t.percentual()).isEqualTo(100.0));
    }

    // ------------------------------------------------------------ panorama

    /**
     * O panorama guarda so {@code item_id}; devolver veiculo e link resolvidos
     * e obrigacao desta API, e o criterio de aceite 6 do contrato do frontend.
     */
    @Test
    void panoramaResolveIdsDeFonteParaVeiculoELink() {
        // Os mocks nascem ANTES de qualquer when(): cria-los dentro de um
        // thenReturn() abriria um stubbing dentro de outro, e o Mockito
        // interrompe com UnfinishedStubbing.
        var doPanorama = panorama("negativo", 3,
                "[{\"id\":\"t1\",\"tema\":\"saude\",\"ocorrencias\":2,"
                        + "\"fontes\":[\"art_1__partido_13\"]}]",
                "[\"art_1__partido_13\"]");
        var aFonte = item("art_1__partido_13", "[]", "G1", "https://g1/1");

        when(panoramas.janelaMaisRecente(anyString(), anyInt())).thenReturn("2026-08");
        when(panoramas.buscarMaisRecentes(anyString(), anyInt(), anyString()))
                .thenReturn(List.of(doPanorama));
        when(itens.buscarPorIds(any())).thenReturn(List.of(aFonte));

        var resposta = service.panorama(POR_PARTIDO);

        assertThat(resposta.janela()).isEqualTo("2026-08");
        assertThat(resposta.eixos()).singleElement().satisfies(e -> {
            assertThat(e.eixo()).isEqualTo("negativo");
            assertThat(e.versao()).isEqualTo(3);
            assertThat(e.fontes()).singleElement().satisfies(f -> {
                assertThat(f.veiculo()).isEqualTo("G1");
                assertThat(f.url()).isEqualTo("https://g1/1");
            });
            assertThat(e.temas()).singleElement().satisfies(t -> {
                assertThat(t.tema()).isEqualTo("saude");
                assertThat(t.ocorrencias()).isEqualTo(2);
                assertThat(t.fontes()).extracting("veiculo").containsExactly("G1");
            });
        });
    }

    /**
     * Id citado pelo panorama sem item correspondente continua na lista, so
     * com o id. Some-lo faria o texto citar uma materia que a tela nao mostra
     * -- e o analyser ja registra esse caso como aviso.
     */
    @Test
    void fonteInexistenteApareceComOIdEmVezDeSumir() {
        var semFonte = panorama("neutro", 1, "[]", "[\"art_28__partido_13\"]");

        when(panoramas.janelaMaisRecente(anyString(), anyInt())).thenReturn("2026-08");
        when(panoramas.buscarMaisRecentes(anyString(), anyInt(), anyString()))
                .thenReturn(List.of(semFonte));
        when(itens.buscarPorIds(any())).thenReturn(List.of());

        var resposta = service.panorama(POR_PARTIDO);

        assertThat(resposta.eixos()).singleElement()
                .satisfies(e -> assertThat(e.fontes()).singleElement().satisfies(f -> {
                    assertThat(f.itemId()).isEqualTo("art_28__partido_13");
                    assertThat(f.url()).isNull();
                }));
    }

    /** Entidade sem panorama nenhum e resposta vazia, nao erro. */
    @Test
    void entidadeSemPanoramaDevolveListaVazia() {
        when(panoramas.janelaMaisRecente(anyString(), anyInt())).thenReturn(null);

        var resposta = service.panorama(POR_PARTIDO);

        assertThat(resposta.janela()).isNull();
        assertThat(resposta.eixos()).isEmpty();
        assertThat(resposta.nome()).isEqualTo("PT");
    }

    // ------------------------------------------------------------- comuns

    /** Mesma regra das outras consultas: politico OU partido, nunca os dois. */
    @Test
    void recusaOsDoisIdentificadoresJuntos() {
        var ambos = new ConsultaRequest("123456789012", 13, null, null, null,
                null, null, null, null);

        assertThatThrownBy(() -> service.porTema(ambos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente um dos dois");
        assertThatThrownBy(() -> service.panorama(ambos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente um dos dois");
    }

    // --------------------------------------------------------------- apoio

    /** A consulta de agregacao projeta so a coluna de temas, nao a entidade. */
    private void materias(String... temasJsonPorMateria) {
        when(itens.listarTemasParaAgregacao(anyString(), anyInt(), any(), any(), any()))
                .thenReturn(List.of(temasJsonPorMateria));
    }

    private static String temas(String... nomes) {
        return "[" + String.join(",",
                Arrays.stream(nomes).map(n -> "\"" + n + "\"").toList()) + "]";
    }

    private static String semTemas() {
        return "[]";
    }

    private static AnaliseItem item(String id, String temasJson, String veiculo, String url) {
        var midia = mock(Midia.class);
        when(midia.getNome()).thenReturn(veiculo);
        var item = mock(AnaliseItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getTemasJson()).thenReturn(temasJson);
        when(item.getMidia()).thenReturn(midia);
        when(item.getLinkNoticia()).thenReturn(url);
        return item;
    }

    private static Panorama panorama(String eixo, int versao, String temasJson,
                                     String fontesJson) {
        var p = mock(Panorama.class);
        when(p.getEixo()).thenReturn(eixo);
        when(p.getVersao()).thenReturn(versao);
        when(p.getTotalItens()).thenReturn(1);
        when(p.getTexto()).thenReturn("Texto consolidado.");
        when(p.getTemasJson()).thenReturn(temasJson);
        when(p.getFontesJson()).thenReturn(fontesJson);
        when(p.getAvisosJson()).thenReturn("[]");
        return p;
    }
}
