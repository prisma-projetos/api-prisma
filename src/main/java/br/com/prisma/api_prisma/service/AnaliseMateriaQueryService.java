package br.com.prisma.api_prisma.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import br.com.prisma.api_prisma.dto.AnaliseMateriaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorTemaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorTemaResponse.TemaContagem;
import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.PanoramaResponse;
import br.com.prisma.api_prisma.dto.PanoramaResponse.Fonte;
import br.com.prisma.api_prisma.dto.PanoramaResponse.PanoramaEixo;
import br.com.prisma.api_prisma.dto.PanoramaResponse.TemaPanorama;
import br.com.prisma.api_prisma.dto.ProvenienciaAnalise;
import br.com.prisma.api_prisma.model.AnaliseItem;
import br.com.prisma.api_prisma.model.Panorama;
import br.com.prisma.api_prisma.repository.AnaliseItemRepository;
import br.com.prisma.api_prisma.repository.PanoramaRepository;

/**
 * O que a imprensa publicou sobre a entidade, no nivel da materia.
 *
 * <p>Complementa {@link AnaliseQueryService}, que trabalha no nivel do
 * paragrafo: aquele conta sentimento vindo do {@code classifier-prisma}, este
 * le o que o {@code analyser-prisma} escreveu -- resumo por materia
 * ({@code analyser_item}) e o texto consolidado do mes
 * ({@code analyser_panorama}). Nao ha dependencia entre os dois.
 *
 * <p><b>Vocabulario.</b> O analyser chama de <b>eixo</b> a polaridade
 * ({@code positivo}/{@code negativo}/{@code neutro}) e de <b>tema</b> o
 * recorte de pauta. Esta classe mantem os dois nomes como o banco os usa: o
 * recorte de pauta e {@link #porTema}, a polaridade aparece como
 * {@code rotulo} na materia e como {@code eixo} no panorama.
 */
@Service
@Transactional(readOnly = true)
public class AnaliseMateriaQueryService {

    /**
     * Limites usados quando o cliente nao informa periodo.
     *
     * <p>{@code INICIO_ABERTO} tem um segundo papel: e para onde a materia sem
     * data de publicacao e trazida no filtro, o que a mantem nos agregados sem
     * recorte e a exclui dos com recorte. Ver {@code FILTRO_PERIODO} em
     * {@link AnaliseItemRepository}.
     */
    private static final OffsetDateTime INICIO_ABERTO =
            OffsetDateTime.parse("1900-01-01T00:00:00Z");
    private static final OffsetDateTime FIM_ABERTO =
            OffsetDateTime.parse("2999-12-31T23:59:59Z");

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    /** Vazio desliga o filtro -- nunca null, ver nota no repositorio. */
    private static final String SEM_FILTRO = "";

    /** Rotulo do grupo de materias sem tema atribuido. */
    private static final String SEM_TEMA = "(tema nao atribuido)";

    private static final TypeReference<List<String>> LISTA_DE_TEXTO =
            new TypeReference<>() {
            };
    private static final TypeReference<List<TemaJson>> LISTA_DE_TEMAS =
            new TypeReference<>() {
            };

    private final CatalogoQueryService catalogo;
    private final AnaliseItemRepository itens;
    private final PanoramaRepository panoramas;

    /**
     * O {@code ObjectMapper} do Boot 4, que e o do Jackson 3
     * ({@code tools.jackson}) -- o Jackson 2 ainda aparece no classpath por
     * dependencia transitiva, mas nao tem bean. Reusar o mapper da aplicacao,
     * em vez de instanciar um aqui, mantem a leitura do JSON do banco sob a
     * mesma configuracao que serializa as respostas.
     */
    private final ObjectMapper json;

    public AnaliseMateriaQueryService(CatalogoQueryService catalogo,
                                      AnaliseItemRepository itens,
                                      PanoramaRepository panoramas,
                                      ObjectMapper json) {
        this.catalogo = catalogo;
        this.itens = itens;
        this.panoramas = panoramas;
        this.json = json;
    }

    /** Materias analisadas, da mais recente para a mais antiga. */
    public Page<AnaliseMateriaResponse> materias(ConsultaRequest request) {
        Alvo alvo = Alvo.resolver(request, catalogo);

        return itens.buscarMaterias(
                        alvo.tituloParaConsulta(),
                        alvo.numeroParaConsulta(),
                        normalizar(request.tema()),
                        normalizar(request.janela()),
                        inicio(request.de()),
                        fim(request.ate()),
                        INICIO_ABERTO,
                        paginacao(request.pagina(), request.tamanho()))
                .map(i -> AnaliseMateriaResponse.from(i, ler(i.getTemasJson(), LISTA_DE_TEXTO)));
    }

    /**
     * Volume por tema. Expoe concentracao de pauta: se quase tudo cai num tema
     * so, o agregado descreve aquele recorte e nao a cobertura toda.
     *
     * <p>A contagem acontece aqui, e nao em {@code group by}, porque os temas
     * de uma materia sao um array JSON -- mesma escolha que
     * {@code AcumuladorMidia} faz em {@link AnaliseQueryService}. A consulta
     * projeta so a coluna de temas: contar materias nao precisa do resumo de
     * cada uma.
     */
    public AnalisePorTemaResponse porTema(ConsultaRequest request) {
        Alvo alvo = Alvo.resolver(request, catalogo);

        List<String> temasPorMateria = itens.listarTemasParaAgregacao(
                alvo.tituloParaConsulta(), alvo.numeroParaConsulta(),
                inicio(request.de()), fim(request.ate()), INICIO_ABERTO);

        // LinkedHashMap para que temas de mesmo volume saiam em ordem estavel
        // entre chamadas -- uma lista que embaralha a cada request e ilegivel.
        Map<String, Long> contagem = new LinkedHashMap<>();
        long semTema = 0;
        for (String bruto : temasPorMateria) {
            List<String> daMateria = ler(bruto, LISTA_DE_TEXTO);
            if (daMateria.isEmpty()) {
                semTema++;
                continue;
            }
            // Distinct por materia: um tema repetido na mesma materia continua
            // sendo uma materia que toca aquele tema, nao duas.
            for (String tema : new LinkedHashSet<>(daMateria)) {
                contagem.merge(tema, 1L, Long::sum);
            }
        }

        long total = temasPorMateria.size();
        List<TemaContagem> temas = new ArrayList<>(contagem.entrySet().stream()
                .map(e -> new TemaContagem(e.getKey(), e.getValue(),
                        percentual(e.getValue(), total)))
                .sorted((a, b) -> Long.compare(b.total(), a.total()))
                .toList());
        if (semTema > 0) {
            temas.add(new TemaContagem(SEM_TEMA, semTema, percentual(semTema, total)));
        }

        List<ProvenienciaAnalise> proveniencia = itens
                .contarPorModelo(alvo.tituloParaConsulta(), alvo.numeroParaConsulta())
                .stream()
                .map(m -> new ProvenienciaAnalise(m.getModelo(), m.getRegraAgregacao(),
                        m.getTotal()))
                .toList();

        return new AnalisePorTemaResponse(
                alvo.tipo(), alvo.identificador(), alvo.nome(),
                total, semTema, temas, proveniencia);
    }

    /**
     * O panorama consolidado da janela, um texto por eixo.
     *
     * <p>Sem {@code janela} no pedido, responde a mais recente que existir --
     * um mes vazio nao e erro, e uma lista de eixos vazia.
     */
    public PanoramaResponse panorama(ConsultaRequest request) {
        Alvo alvo = Alvo.resolver(request, catalogo);
        String titulo = alvo.tituloParaConsulta();
        int numero = alvo.numeroParaConsulta();

        String janela = normalizar(request.janela());
        if (janela.isEmpty()) {
            janela = panoramas.janelaMaisRecente(titulo, numero);
        }
        if (janela == null || janela.isEmpty()) {
            return new PanoramaResponse(alvo.tipo(), alvo.identificador(), alvo.nome(),
                    null, List.of());
        }

        List<Consolidado> consolidados = panoramas.buscarMaisRecentes(titulo, numero, janela)
                .stream()
                .map(this::lerConsolidado)
                .toList();

        // Uma unica ida ao banco resolve as fontes de todos os eixos e de todos
        // os temas: o analyser guarda apenas item_id, e o contrato com o
        // frontend manda esta API devolver veiculo e link resolvidos.
        Map<String, Fonte> resolvidas = resolverFontes(consolidados);

        List<PanoramaEixo> eixos = consolidados.stream()
                .map(c -> c.paraDto(resolvidas))
                .toList();

        return new PanoramaResponse(alvo.tipo(), alvo.identificador(), alvo.nome(),
                janela, eixos);
    }

    // ---------------------------------------------------------------- apoio

    /**
     * Um panorama com seus campos JSON ja desserializados.
     *
     * <p>Existe para que cada blob seja lido uma vez so. Sem ele, coletar os
     * ids de fonte e depois montar o DTO significa parsear os mesmos
     * {@code temas_json} e {@code fontes_json} duas vezes por eixo.
     */
    private record Consolidado(Panorama panorama, List<TemaJson> temas,
                               List<String> fontes, List<String> avisos) {

        /** Todo item_id citado por este eixo, direto ou dentro de um tema. */
        Set<String> idsDeFonte() {
            Set<String> ids = new LinkedHashSet<>(fontes);
            temas.forEach(t -> ids.addAll(t.fontesOuVazio()));
            return ids;
        }

        PanoramaEixo paraDto(Map<String, Fonte> resolvidas) {
            return new PanoramaEixo(
                    panorama.getEixo(),
                    ouZero(panorama.getVersao()),
                    ouZero(panorama.getTotalItens()),
                    panorama.getTexto(),
                    temas.stream()
                            .map(t -> new TemaPanorama(t.tema(), t.ocorrenciasOuZero(),
                                    resolver(t.fontesOuVazio(), resolvidas)))
                            .toList(),
                    resolver(fontes, resolvidas),
                    avisos,
                    panorama.getModelo(),
                    panorama.getCriadoEm());
        }
    }

    /** Forma do {@code temas_json} do panorama -- objeto, nao string. */
    private record TemaJson(String id, String tema, Integer ocorrencias, List<String> fontes) {

        int ocorrenciasOuZero() {
            return ouZero(ocorrencias);
        }

        List<String> fontesOuVazio() {
            return fontes == null ? List.of() : fontes;
        }
    }

    private Consolidado lerConsolidado(Panorama p) {
        return new Consolidado(p,
                ler(p.getTemasJson(), LISTA_DE_TEMAS),
                ler(p.getFontesJson(), LISTA_DE_TEXTO),
                ler(p.getAvisosJson(), LISTA_DE_TEXTO));
    }

    private Map<String, Fonte> resolverFontes(List<Consolidado> consolidados) {
        Set<String> ids = new LinkedHashSet<>();
        consolidados.forEach(c -> ids.addAll(c.idsDeFonte()));
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Fonte> resolvidas = new LinkedHashMap<>();
        for (AnaliseItem i : itens.buscarPorIds(ids)) {
            var midia = i.getMidia();
            resolvidas.put(i.getId(), new Fonte(
                    i.getId(),
                    midia != null ? midia.getNome() : null,
                    i.getLinkNoticia(),
                    i.getDataPublicacao()));
        }
        return resolvidas;
    }

    /**
     * Um id citado pelo panorama e sem item correspondente vira fonte so com o
     * id, em vez de sumir da lista. O analyser ja registra esse caso como
     * aviso ({@code fonte_inexistente}); esconde-lo aqui faria o texto citar
     * uma materia que a tela nao mostra.
     */
    private static List<Fonte> resolver(List<String> ids, Map<String, Fonte> resolvidas) {
        return ids.stream()
                .map(id -> resolvidas.getOrDefault(id, new Fonte(id, null, null, null)))
                .toList();
    }

    /**
     * JSON gravado por outro componente e entrada externa: um formato
     * inesperado devolve lista vazia em vez de derrubar a consulta inteira.
     * Uma materia com tema ilegivel some do recorte de pauta; a resposta
     * inteira sumindo seria pior.
     */
    private <T> List<T> ler(String bruto, TypeReference<List<T>> tipo) {
        if (bruto == null || bruto.isBlank()) {
            return List.of();
        }
        try {
            List<T> lidos = json.readValue(bruto, tipo);
            return lidos == null ? List.of() : lidos.stream().filter(t -> t != null).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static int ouZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? SEM_FILTRO : valor.trim();
    }

    private static Pageable paginacao(Integer pagina, Integer tamanho) {
        int p = pagina == null || pagina < 0 ? 0 : pagina;
        int t = tamanho == null || tamanho < 1
                ? TAMANHO_PADRAO
                : Math.min(tamanho, TAMANHO_MAXIMO);
        return PageRequest.of(p, t);
    }

    /**
     * A data da materia e {@code timestamptz}. Um recorte informado como dia
     * civil e interpretado no fuso da aplicacao ({@code America/Fortaleza} em
     * producao), que e o fuso em que quem consulta pensa a data.
     */
    private static OffsetDateTime inicio(LocalDate de) {
        return de == null
                ? INICIO_ABERTO
                : de.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static OffsetDateTime fim(LocalDate ate) {
        return ate == null
                ? FIM_ABERTO
                : ate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 10000.0 / total) / 100.0;
    }
}
