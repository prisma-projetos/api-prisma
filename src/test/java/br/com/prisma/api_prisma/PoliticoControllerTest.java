package br.com.prisma.api_prisma;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.prisma.api_prisma.controller.PoliticoController;
import br.com.prisma.api_prisma.dto.CandidatoResumoResponse;
import br.com.prisma.api_prisma.security.ApiKeyFilter;
import br.com.prisma.api_prisma.security.SecurityConfig;
import br.com.prisma.api_prisma.service.CatalogoQueryService;

@WebMvcTest(PoliticoController.class)
@Import({ SecurityConfig.class, ApiKeyFilter.class })
@TestPropertySource(properties = "api.security.key=chave-de-teste")
class PoliticoControllerTest {

    private static final String KEY_HEADER = "X-API-KEY";
    private static final String KEY = "chave-de-teste";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogoQueryService service;

    /** O filtro chega ate o servico exatamente como recebido, sem alteracao. */
    @Test
    void listaFiltraPorCargo() throws Exception {
        var candidato = new CandidatoResumoResponse(
                "122418060191", "LUIZ INÁCIO LULA DA SILVA", "PRESIDENTE",
                "https://exemplo.com/foto.jpg");

        when(service.listarPoliticos(isNull(), eq(13), isNull(), eq("PRESIDENTE"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(candidato), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY)
                        .param("nrPartido", "13")
                        .param("cargo", "PRESIDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tituloEleitoral").value("122418060191"))
                .andExpect(jsonPath("$.content[0].cargo").value("PRESIDENTE"));
    }

    /** Sem 'cargo' na query string, o parametro chega nulo ao servico -- filtro desligado. */
    @Test
    void listaSemCargoNaoFiltra() throws Exception {
        when(service.listarPoliticos(any(), any(), any(), isNull(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY))
                .andExpect(status().isOk());
    }

    @Test
    void exigeChaveDeApi() throws Exception {
        mockMvc.perform(get("/api/politicos").param("cargo", "PRESIDENTE"))
                .andExpect(status().isUnauthorized());
    }
}
