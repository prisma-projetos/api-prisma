package br.com.prisma.api_prisma;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
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
                "https://exemplo.com/foto.jpg", 13, "PT");

        when(service.listarPoliticos(isNull(), eq(13), isNull(), eq("PRESIDENTE"), isNull(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(candidato), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY)
                        .param("nrPartido", "13")
                        .param("cargo", "PRESIDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tituloEleitoral").value("122418060191"))
                .andExpect(jsonPath("$.content[0].cargo").value("PRESIDENTE"))
                .andExpect(jsonPath("$.content[0].nrPartido").value(13))
                .andExpect(jsonPath("$.content[0].siglaPartido").value("PT"));
    }

    /** Sem 'cargo' na query string, o parametro chega nulo ao servico -- filtro desligado. */
    @Test
    void listaSemCargoNaoFiltra() throws Exception {
        when(service.listarPoliticos(any(), any(), any(), isNull(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY))
                .andExpect(status().isOk());
    }

    /** A UF chega ao servico como recebida; a normalizacao para maiusculo fica no servico. */
    @Test
    void listaFiltraPorUf() throws Exception {
        var candidato = new CandidatoResumoResponse(
                "013637562380", "ADAILTON ANTUNES FERREIRA", "GOVERNADOR",
                "https://exemplo.com/foto.jpg", 55, "PSD");

        when(service.listarPoliticos(isNull(), isNull(), eq(2026), isNull(), eq("ro"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(candidato), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY)
                        .param("anoEleicao", "2026")
                        .param("uf", "ro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tituloEleitoral").value("013637562380"))
                .andExpect(jsonPath("$.content[0].nrPartido").value(55))
                .andExpect(jsonPath("$.content[0].siglaPartido").value("PSD"));
    }

    /** Cada item traz o partido da propria candidatura; sem partido, os campos vem nulos. */
    @Test
    void listaTrazPartidoDeCadaCandidatura() throws Exception {
        var candidatos = List.of(
                new CandidatoResumoResponse("013637562380", "ADAILTON ANTUNES FERREIRA",
                        "GOVERNADOR", "https://exemplo.com/a.jpg", 55, "PSD"),
                new CandidatoResumoResponse("062567640809", "ABIMAEL DOS SANTOS PEREIRA LEAL",
                        "DEPUTADO FEDERAL", "https://exemplo.com/b.jpg", 22, "PL"),
                new CandidatoResumoResponse("003013841872", "ABMAEL BORGES DA SILVEIRA",
                        "DEPUTADO FEDERAL", "https://exemplo.com/c.jpg", null, null));

        when(service.listarPoliticos(isNull(), isNull(), eq(2026), isNull(), isNull(), any(), any()))
                .thenReturn(new PageImpl<>(candidatos, PageRequest.of(0, 20), 3));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY)
                        .param("anoEleicao", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nrPartido").value(55))
                .andExpect(jsonPath("$.content[0].siglaPartido").value("PSD"))
                .andExpect(jsonPath("$.content[1].nrPartido").value(22))
                .andExpect(jsonPath("$.content[1].siglaPartido").value("PL"))
                .andExpect(jsonPath("$.content[2].nrPartido").isEmpty())
                .andExpect(jsonPath("$.content[2].siglaPartido").isEmpty());
    }

    @Test
    void listaFiltraPorPartido() throws Exception {
        var candidatos = List.of(
                new CandidatoResumoResponse("013637562380", "ADAILTON ANTUNES FERREIRA",
                        "GOVERNADOR", "https://exemplo.com/a.jpg", 55, "PSD"),
                new CandidatoResumoResponse("111111111111", "BEATRIZ EXEMPLO",
                        "SENADOR", "https://exemplo.com/b.jpg", 55, "PSD"));

        when(service.listarPoliticos(isNull(), eq(55), eq(2026), isNull(), isNull(), any(), any()))
                .thenReturn(new PageImpl<>(candidatos, PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY)
                        .param("nrPartido", "55")
                        .param("anoEleicao", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].nrPartido", everyItem(is(55))))
                .andExpect(jsonPath("$.content[*].siglaPartido", everyItem(is("PSD"))));
    }

    /** q, cargo e uf juntos continuam chegando ao servico, e o partido vem no item. */
    @Test
    void listaCombinaFiltrosComPartido() throws Exception {
        var candidato = new CandidatoResumoResponse(
                "013637562380", "ADAILTON ANTUNES FERREIRA", "GOVERNADOR",
                "https://exemplo.com/foto.jpg", 55, "PSD");

        when(service.listarPoliticos(eq("adailton"), isNull(), eq(2026), eq("GOVERNADOR"), eq("RO"),
                any(), any()))
                .thenReturn(new PageImpl<>(List.of(candidato), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/politicos")
                        .header(KEY_HEADER, KEY)
                        .param("q", "adailton")
                        .param("anoEleicao", "2026")
                        .param("cargo", "GOVERNADOR")
                        .param("uf", "RO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("ADAILTON ANTUNES FERREIRA"))
                .andExpect(jsonPath("$.content[0].cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$.content[0].nrPartido").value(55))
                .andExpect(jsonPath("$.content[0].siglaPartido").value("PSD"));
    }

    @Test
    void exigeChaveDeApi() throws Exception {
        mockMvc.perform(get("/api/politicos").param("cargo", "PRESIDENTE"))
                .andExpect(status().isUnauthorized());
    }
}
