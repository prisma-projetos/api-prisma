package br.com.prisma.api_prisma.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
