package br.com.prisma.api_prisma.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.prisma.api_prisma.dto.AnaliseMateriaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorEixoResponse;
import br.com.prisma.api_prisma.dto.AnalisePorEixoResponse.EixoContagem;
import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse.Proveniencia;
import br.com.prisma.api_prisma.repository.AnaliseMateriaRepository;
import br.com.prisma.api_prisma.repository.AnaliseMateriaRepository.ContagemPorEixo;

/**
 * O que a imprensa publicou sobre a entidade, no nivel da materia.
 *
 * <p>Complementa {@link AnaliseQueryService}, que trabalha no nivel do
 * paragrafo: aquele conta sentimento, este descreve a materia (eixo, titulo,
 * resumo). Nao ha dependencia entre os dois -- uma entidade pode ter analise
 * de materia sem classificacao de paragrafo, e vice-versa.
 */
@Service
@Transactional(readOnly = true)
public class AnaliseMateriaQueryService {

    /** Limites usados quando o cliente nao informa periodo. */
    private static final LocalDateTime INICIO_ABERTO = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime FIM_ABERTO = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    /** Vazio traz todos os eixos -- nunca null, ver nota no repositorio. */
    private static final String TODOS_OS_EIXOS = "";

    /** Rotulo do grupo de materias sem eixo atribuido. */
    private static final String SEM_EIXO = "(eixo nao atribuido)";

    private final CatalogoQueryService catalogo;
    private final AnaliseMateriaRepository analises;

    public AnaliseMateriaQueryService(CatalogoQueryService catalogo,
                                      AnaliseMateriaRepository analises) {
        this.catalogo = catalogo;
        this.analises = analises;
    }

    /** Materias analisadas, da mais recente para a mais antiga. */
    public Page<AnaliseMateriaResponse> materias(ConsultaRequest request) {
        Alvo alvo = Alvo.resolver(request, catalogo);

        return analises.buscarMaterias(
                        alvo.tituloParaConsulta(),
                        alvo.numeroParaConsulta(),
                        normalizarEixo(request.eixo()),
                        inicio(request.de()),
                        fim(request.ate()),
                        paginacao(request.pagina(), request.tamanho()))
                .map(AnaliseMateriaResponse::from);
    }

    /**
     * Volume por eixo tematico. Expoe concentracao de pauta: se quase tudo cai
     * num eixo so, o agregado descreve aquele recorte e nao a cobertura toda.
     */
    public AnalisePorEixoResponse porEixo(ConsultaRequest request) {
        Alvo alvo = Alvo.resolver(request, catalogo);

        List<ContagemPorEixo> linhas = analises.contarPorEixo(
                alvo.tituloParaConsulta(), alvo.numeroParaConsulta(),
                inicio(request.de()), fim(request.ate()));

        long total = linhas.stream().mapToLong(ContagemPorEixo::getTotal).sum();
        long semEixo = linhas.stream()
                .filter(l -> l.getEixo() == null)
                .mapToLong(ContagemPorEixo::getTotal)
                .sum();

        List<EixoContagem> eixos = new ArrayList<>(linhas.stream()
                .filter(l -> l.getEixo() != null)
                .map(l -> new EixoContagem(l.getEixo(), l.getTotal(),
                        percentual(l.getTotal(), total)))
                .sorted((a, b) -> Long.compare(b.total(), a.total()))
                .toList());
        if (semEixo > 0) {
            eixos.add(new EixoContagem(SEM_EIXO, semEixo, percentual(semEixo, total)));
        }

        List<Proveniencia> proveniencia = analises
                .contarPorModelo(alvo.tituloParaConsulta(), alvo.numeroParaConsulta())
                .stream()
                .map(m -> new Proveniencia(m.getMetodo(), m.getPipelineVersion(), m.getTotal()))
                .toList();

        return new AnalisePorEixoResponse(
                alvo.tipo(), alvo.identificador(), alvo.nome(),
                total, semEixo, eixos, proveniencia);
    }

    // ---------------------------------------------------------------- apoio

    /**
     * Sem validacao contra lista fechada: o vocabulario de eixos ainda nao foi
     * definido pelo analyser. Um eixo desconhecido devolve pagina vazia, que e
     * honesto -- diferente de sentimento, que tem os tres valores fixados.
     */
    private static String normalizarEixo(String eixo) {
        return eixo == null || eixo.isBlank() ? TODOS_OS_EIXOS : eixo.toLowerCase();
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
}
