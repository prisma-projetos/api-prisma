package br.com.prisma.api_prisma.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.prisma.api_prisma.dto.CandidaturaResponse;
import br.com.prisma.api_prisma.dto.CandidatoResumoResponse;
import br.com.prisma.api_prisma.dto.PartidoResponse;
import br.com.prisma.api_prisma.dto.PoliticoDetalheResponse;
import br.com.prisma.api_prisma.dto.PoliticoResponse;
import br.com.prisma.api_prisma.exception.NotFoundException;
import br.com.prisma.api_prisma.model.Partido;
import br.com.prisma.api_prisma.model.Politico;
import br.com.prisma.api_prisma.repository.CandidaturaRepository;
import br.com.prisma.api_prisma.repository.PartidoRepository;
import br.com.prisma.api_prisma.repository.PoliticoRepository;

/**
 * Quem existe: politicos, partidos e o historico de candidaturas.
 *
 * <p>Nao sabe nada sobre a analise das noticias -- isso e de
 * {@link AnaliseQueryService}.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoQueryService {

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private final PoliticoRepository politicos;
    private final PartidoRepository partidos;
    private final CandidaturaRepository candidaturas;

    public CatalogoQueryService(PoliticoRepository politicos,
                                PartidoRepository partidos,
                                CandidaturaRepository candidaturas) {
        this.politicos = politicos;
        this.partidos = partidos;
        this.candidaturas = candidaturas;
    }

    /**
     * Lista candidaturas (nao pessoas), resumidas para a pagina do partido:
     * titulo, nome, cargo e urlFoto. Dados completos ficam em /detalhe.
     * {@code nrPartido}/{@code anoEleicao}/{@code cargo} nulos nao filtram.
     * Sem {@code anoEleicao}, um politico com candidaturas em varias eleicoes
     * aparece uma vez por eleicao.
     */
    public Page<CandidatoResumoResponse> listarPoliticos(String termo, Integer nrPartido,
                                                           Integer anoEleicao, String cargo,
                                                           Integer pagina, Integer tamanho) {
        return candidaturas.buscar(padraoDeBusca(termo), nrPartido, anoEleicao,
                        normalizarCargo(cargo), paginacao(pagina, tamanho))
                .map(CandidatoResumoResponse::from);
    }

    /** Nulo/vazio desliga o filtro; o resto vira minusculo para casar com o repositorio. */
    private static String normalizarCargo(String cargo) {
        return cargo == null || cargo.isBlank() ? null : cargo.trim().toLowerCase();
    }

    public List<PartidoResponse> listarPartidos(String termo) {
        return partidos.buscar(padraoDeBusca(termo)).stream()
                .map(PartidoResponse::from)
                .toList();
    }

    /**
     * Politico com suas candidaturas. Cargo, UF e partido vivem na candidatura,
     * nao na pessoa: mudam de uma eleicao para outra.
     */
    public PoliticoDetalheResponse detalharPolitico(String tituloEleitoral) {
        Politico politico = carregarPolitico(tituloEleitoral);
        List<CandidaturaResponse> historico = candidaturas.doPolitico(tituloEleitoral).stream()
                .map(CandidaturaResponse::from)
                .toList();
        return new PoliticoDetalheResponse(PoliticoResponse.from(politico), historico);
    }

    public PartidoResponse detalharPartido(Integer numero) {
        return PartidoResponse.from(carregarPartido(numero));
    }

    // ---------------------------------------------------------------- apoio

    Politico carregarPolitico(String tituloEleitoral) {
        if (tituloEleitoral == null || tituloEleitoral.isBlank()) {
            throw new IllegalArgumentException("Informe 'tituloEleitoral'.");
        }
        return politicos.findById(tituloEleitoral)
                .orElseThrow(() -> new NotFoundException(
                        "Politico com titulo eleitoral '" + tituloEleitoral
                                + "' nao encontrado."));
    }

    Partido carregarPartido(Integer numero) {
        if (numero == null) {
            throw new IllegalArgumentException("Informe 'nrPartido'.");
        }
        return partidos.findById(numero)
                .orElseThrow(() -> new NotFoundException(
                        "Partido numero " + numero + " nao encontrado."));
    }

    /** Termo vazio vira "%", que casa com tudo -- e nunca null, ver repositorio. */
    private static String padraoDeBusca(String termo) {
        return termo == null || termo.isBlank()
                ? "%"
                : "%" + termo.toLowerCase() + "%";
    }

    private static Pageable paginacao(Integer pagina, Integer tamanho) {
        int p = pagina == null || pagina < 0 ? 0 : pagina;
        int t = tamanho == null || tamanho < 1
                ? TAMANHO_PADRAO
                : Math.min(tamanho, TAMANHO_MAXIMO);
        return PageRequest.of(p, t);
    }
}
