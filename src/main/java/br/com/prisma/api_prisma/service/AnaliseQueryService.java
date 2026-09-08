package br.com.prisma.api_prisma.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.MencaoResponse;
import br.com.prisma.api_prisma.dto.SentimentoPorMidiaResponse;
import br.com.prisma.api_prisma.dto.SentimentoPorMidiaResponse.MidiaSentimento;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse.ContagemSentimento;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse.Proveniencia;
import br.com.prisma.api_prisma.model.Classificacao;
import br.com.prisma.api_prisma.repository.ClassificacaoRepository;
import br.com.prisma.api_prisma.repository.ClassificacaoRepository.ContagemPorMidia;

/**
 * O que a imprensa disse: sentimento agregado e os paragrafos que o sustentam.
 *
 * <p>Toda consulta e sobre um {@link Alvo} -- politico ou partido -- resolvido
 * a partir do corpo da requisicao.
 */
@Service
@Transactional(readOnly = true)
public class AnaliseQueryService {

    /** Limites usados quando o cliente nao informa periodo. */
    private static final LocalDateTime INICIO_ABERTO = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime FIM_ABERTO = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    /** Rotulo do grupo de classificacoes sem veiculo associado. */
    private static final String SEM_VEICULO = "(veiculo nao identificado)";

    /** Vazio traz todos os sentimentos -- nunca null, ver nota no repositorio. */
    private static final String TODOS_OS_SENTIMENTOS = "";

    private static final List<String> SENTIMENTOS_VALIDOS = List.of(
            Classificacao.SENTIMENTO_POSITIVO,
            Classificacao.SENTIMENTO_NEUTRO,
            Classificacao.SENTIMENTO_NEGATIVO);

    private final CatalogoQueryService catalogo;
    private final ClassificacaoRepository classificacoes;

    public AnaliseQueryService(CatalogoQueryService catalogo,
                               ClassificacaoRepository classificacoes) {
        this.catalogo = catalogo;
        this.classificacoes = classificacoes;
    }

    /**
     * Sentimento agregado. A unidade contada e o paragrafo, nao a noticia: uma
     * materia longa pesa mais que uma nota curta.
     */
    public SentimentoResumoResponse resumoSentimento(ConsultaRequest request) {
        Alvo alvo = resolverAlvo(request);

        var contagens = classificacoes.contarPorSentimento(
                alvo.tituloParaConsulta(), alvo.numeroParaConsulta(),
                inicio(request.de()), fim(request.ate()));

        long total = contagens.stream().mapToLong(c -> c.getTotal()).sum();

        List<ContagemSentimento> porSentimento = contagens.stream()
                .map(c -> new ContagemSentimento(
                        c.getSentimento(),
                        c.getTotal(),
                        percentual(c.getTotal(), total),
                        c.getScoreMedio()))
                .sorted(maiorVolumePrimeiro(ContagemSentimento::total))
                .toList();

        List<Proveniencia> proveniencia = classificacoes
                .contarPorModelo(alvo.tituloParaConsulta(), alvo.numeroParaConsulta())
                .stream()
                .map(m -> new Proveniencia(m.getMetodo(), m.getPipelineVersion(), m.getTotal()))
                .toList();

        return new SentimentoResumoResponse(
                alvo.tipo(), alvo.identificador(), alvo.nome(),
                request.de(), request.ate(), total, porSentimento, proveniencia);
    }

    /**
     * Sentimento aberto por veiculo, para expor concentracao de fontes: se
     * quase todo o volume vem de um unico veiculo, o agregado descreve a linha
     * editorial dele, nao a cobertura em geral.
     */
    public SentimentoPorMidiaResponse sentimentoPorMidia(ConsultaRequest request) {
        Alvo alvo = resolverAlvo(request);

        var linhas = classificacoes.contarPorMidia(
                alvo.tituloParaConsulta(), alvo.numeroParaConsulta(),
                inicio(request.de()), fim(request.ate()));

        Map<Integer, AcumuladorMidia> identificados = new LinkedHashMap<>();
        AcumuladorMidia semVeiculo = new AcumuladorMidia(null, SEM_VEICULO);

        for (ContagemPorMidia linha : linhas) {
            AcumuladorMidia destino = linha.getIdMidia() == null
                    ? semVeiculo
                    : identificados.computeIfAbsent(linha.getIdMidia(),
                            id -> new AcumuladorMidia(id, linha.getNomeMidia()));
            destino.somar(linha.getSentimento(), linha.getTotal());
        }

        long total = identificados.values().stream().mapToLong(AcumuladorMidia::total).sum()
                + semVeiculo.total();

        List<MidiaSentimento> midias = new ArrayList<>(identificados.values().stream()
                .map(a -> a.paraDto(total))
                .sorted(maiorVolumePrimeiro(MidiaSentimento::total))
                .toList());
        if (semVeiculo.total() > 0) {
            midias.add(semVeiculo.paraDto(total));
        }

        return new SentimentoPorMidiaResponse(
                alvo.tipo(), alvo.identificador(), alvo.nome(),
                identificados.size(), total, semVeiculo.total(), midias);
    }

    /** Paragrafos analisados, com o link da noticia de origem. */
    public Page<MencaoResponse> mencoes(ConsultaRequest request) {
        Alvo alvo = resolverAlvo(request);

        return classificacoes.buscarMencoes(
                        alvo.tituloParaConsulta(),
                        alvo.numeroParaConsulta(),
                        normalizarSentimento(request.sentimento()),
                        inicio(request.de()),
                        fim(request.ate()),
                        paginacao(request.pagina(), request.tamanho()))
                .map(MencaoResponse::from);
    }

    // ---------------------------------------------------------------- apoio

    /** Exige exatamente um identificador: politico OU partido. */
    private Alvo resolverAlvo(ConsultaRequest request) {
        return Alvo.resolver(request, catalogo);
    }

    private static String normalizarSentimento(String sentimento) {
        if (sentimento == null || sentimento.isBlank()) {
            return TODOS_OS_SENTIMENTOS;
        }
        String normalizado = sentimento.toLowerCase();
        if (!SENTIMENTOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "sentimento deve ser 'positivo', 'neutro' ou 'negativo'.");
        }
        return normalizado;
    }

    private static Pageable paginacao(Integer pagina, Integer tamanho) {
        int p = pagina == null || pagina < 0 ? 0 : pagina;
        int t = tamanho == null || tamanho < 1
                ? TAMANHO_PADRAO
                : Math.min(tamanho, TAMANHO_MAXIMO);
        return PageRequest.of(p, t);
    }

    private static LocalDateTime inicio(LocalDate de) {
        return de == null ? INICIO_ABERTO : de.atStartOfDay();
    }

    private static LocalDateTime fim(LocalDate ate) {
        return ate == null ? FIM_ABERTO : ate.atTime(23, 59, 59);
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 10000.0 / total) / 100.0;
    }

    private static <T> Comparator<T> maiorVolumePrimeiro(ToLongFunction<T> volume) {
        return (a, b) -> Long.compare(volume.applyAsLong(b), volume.applyAsLong(a));
    }

    /** Soma os sentimentos de um veiculo enquanto as linhas sao percorridas. */
    private static final class AcumuladorMidia {

        private final Integer idMidia;
        private final String nomeMidia;
        private long positivo;
        private long neutro;
        private long negativo;

        private AcumuladorMidia(Integer idMidia, String nomeMidia) {
            this.idMidia = idMidia;
            this.nomeMidia = nomeMidia;
        }

        private void somar(String sentimento, long quantidade) {
            if (Classificacao.SENTIMENTO_POSITIVO.equals(sentimento)) {
                positivo += quantidade;
            } else if (Classificacao.SENTIMENTO_NEGATIVO.equals(sentimento)) {
                negativo += quantidade;
            } else {
                neutro += quantidade;
            }
        }

        private long total() {
            return positivo + neutro + negativo;
        }

        private MidiaSentimento paraDto(long totalGeral) {
            return new MidiaSentimento(idMidia, nomeMidia, total(),
                    percentual(total(), totalGeral), positivo, neutro, negativo);
        }
    }
}
