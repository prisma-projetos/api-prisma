package br.com.prisma.api_prisma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.prisma.api_prisma.dto.CandidatoResumoResponse;
import br.com.prisma.api_prisma.model.Candidatura;
import br.com.prisma.api_prisma.model.Partido;
import br.com.prisma.api_prisma.model.Politico;
import br.com.prisma.api_prisma.repository.CandidaturaRepository;
import br.com.prisma.api_prisma.repository.PartidoRepository;
import br.com.prisma.api_prisma.repository.PoliticoRepository;

class CatalogoQueryServiceTest {

    private final CandidaturaRepository candidaturas = mock(CandidaturaRepository.class);
    private final CatalogoQueryService service = new CatalogoQueryService(
            mock(PoliticoRepository.class), mock(PartidoRepository.class), candidaturas);

    /** A UF vira maiuscula, como a coluna sg_uf; vazia desliga o filtro. */
    @Test
    void normalizaUf() {
        when(candidaturas.buscar(anyString(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listarPoliticos(null, null, 2026, null, " ro ", 0, 20);
        verify(candidaturas).buscar(eq("%"), isNull(), eq(2026), isNull(), eq("RO"), any(Pageable.class));

        service.listarPoliticos(null, null, 2026, null, " ", 0, 20);
        verify(candidaturas).buscar(eq("%"), isNull(), eq(2026), isNull(), isNull(), any(Pageable.class));
    }

    /** O partido do item e o da candidatura; candidatura sem partido vira campos nulos. */
    @Test
    void listaTrazPartidoDaCandidatura() {
        var comPartido = candidatura(1L, "013637562380", "ADAILTON ANTUNES FERREIRA",
                partido(55, "PSD"));
        var semPartido = candidatura(2L, "003013841872", "ABMAEL BORGES DA SILVEIRA", null);
        when(candidaturas.buscar(anyString(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comPartido, semPartido)));

        List<CandidatoResumoResponse> itens =
                service.listarPoliticos(null, null, 2026, null, null, 0, 20).getContent();

        assertThat(itens.get(0).nrPartido()).isEqualTo(55);
        assertThat(itens.get(0).siglaPartido()).isEqualTo("PSD");
        assertThat(itens.get(1).nrPartido()).isNull();
        assertThat(itens.get(1).siglaPartido()).isNull();
    }

    private static Partido partido(int numero, String sigla) {
        var partido = new Partido();
        ReflectionTestUtils.setField(partido, "numero", numero);
        ReflectionTestUtils.setField(partido, "sigla", sigla);
        return partido;
    }

    private static Candidatura candidatura(Long sequencial, String titulo, String nome,
                                           Partido partido) {
        var politico = new Politico();
        ReflectionTestUtils.setField(politico, "tituloEleitoral", titulo);
        ReflectionTestUtils.setField(politico, "nome", nome);
        var candidatura = new Candidatura();
        ReflectionTestUtils.setField(candidatura, "sequencial", sequencial);
        ReflectionTestUtils.setField(candidatura, "politico", politico);
        ReflectionTestUtils.setField(candidatura, "partido", partido);
        return candidatura;
    }
}
