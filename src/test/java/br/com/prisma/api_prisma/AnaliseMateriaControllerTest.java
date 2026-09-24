package br.com.prisma.api_prisma;

import static org.mockito.ArgumentMatchers.any;
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

import br.com.prisma.api_prisma.controller.AnaliseMateriaController;
import br.com.prisma.api_prisma.dto.AnaliseMateriaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorTemaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorTemaResponse.TemaContagem;
import br.com.prisma.api_prisma.dto.PanoramaResponse;
import br.com.prisma.api_prisma.dto.PanoramaResponse.Fonte;
import br.com.prisma.api_prisma.dto.PanoramaResponse.PanoramaEixo;
import br.com.prisma.api_prisma.dto.PanoramaResponse.TemaPanorama;
import br.com.prisma.api_prisma.dto.ProvenienciaAnalise;
import br.com.prisma.api_prisma.security.ApiKeyFilter;
import br.com.prisma.api_prisma.security.SecurityConfig;
import br.com.prisma.api_prisma.service.AnaliseMateriaQueryService;

@WebMvcTest(AnaliseMateriaController.class)
@Import({ SecurityConfig.class, ApiKeyFilter.class })
@TestPropertySource(properties = "api.security.key=chave-de-teste")
class AnaliseMateriaControllerTest {

    private static final String KEY_HEADER = "X-API-KEY";
    private static final String KEY = "chave-de-teste";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnaliseMateriaQueryService service;

    @Test
    void exigeChaveDeApi() throws Exception {
        mockMvc.perform(get("/api/analise/materias")
                        .param("nrPartido", "13"))
                .andExpect(status().isUnauthorized());
    }

    /** O resumo e escrito por modelo: nunca circula sem dizer qual. */
    @Test
    void materiaTrazSempreQuemAProduziu() throws Exception {
        var materia = new AnaliseMateriaResponse(
                "art_4242__partido_13", 4242L, "negativo",
                "Texto resumido da materia.", List.of("saude"),
                "https://exemplo.com/noticia", 7, "Diario do Vale", null,
                "2026-08", "gemma-3-27b", "eixo-por-sinal-v2", "resumido");

        when(service.materias(any()))
                .thenReturn(new PageImpl<>(List.of(materia), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/analise/materias")
                        .header(KEY_HEADER, KEY)
                        .param("nrPartido", "13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].itemId").value("art_4242__partido_13"))
                .andExpect(jsonPath("$.content[0].temas[0]").value("saude"))
                .andExpect(jsonPath("$.content[0].rotulo").value("negativo"))
                .andExpect(jsonPath("$.content[0].modelo").value("gemma-3-27b"))
                .andExpect(jsonPath("$.content[0].regraAgregacao").value("eixo-por-sinal-v2"));
    }

    /** Materia sem tema e informacao, nao lixo: aparece como grupo proprio. */
    @Test
    void porTemaExpoeAsMateriasSemTema() throws Exception {
        when(service.porTema(any())).thenReturn(new AnalisePorTemaResponse(
                "partido", "13", "PT", 10, 4,
                List.of(new TemaContagem("saude", 6, 60.0),
                        new TemaContagem("(tema nao atribuido)", 4, 40.0)),
                List.of(new ProvenienciaAnalise("gemma-3-27b", "eixo-por-sinal-v2", 10))));

        mockMvc.perform(get("/api/analise/por-tema")
                        .header(KEY_HEADER, KEY)
                        .param("nrPartido", "13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMaterias").value(10))
                .andExpect(jsonPath("$.semTema").value(4))
                .andExpect(jsonPath("$.temas[0].tema").value("saude"))
                .andExpect(jsonPath("$.proveniencia[0].modelo").value("gemma-3-27b"));
    }

    /** O frontend precisa da fonte navegavel, nao do id cru do analyser. */
    @Test
    void panoramaEntregaFontesResolvidas() throws Exception {
        var fonte = new Fonte("art_1__partido_13", "G1", "https://g1/1", null);
        when(service.panorama(any())).thenReturn(new PanoramaResponse(
                "partido", "13", "PT", "2026-08",
                List.of(new PanoramaEixo("negativo", 3, 1, "Texto consolidado.",
                        List.of(new TemaPanorama("saude", 2, List.of(fonte))),
                        List.of(fonte), List.of(), "gemma-3-27b", null))));

        mockMvc.perform(get("/api/analise/panorama")
                        .header(KEY_HEADER, KEY)
                        .param("nrPartido", "13")
                        .param("janela", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.janela").value("2026-08"))
                .andExpect(jsonPath("$.eixos[0].eixo").value("negativo"))
                .andExpect(jsonPath("$.eixos[0].versao").value(3))
                .andExpect(jsonPath("$.eixos[0].texto").value("Texto consolidado."))
                .andExpect(jsonPath("$.eixos[0].fontes[0].veiculo").value("G1"))
                .andExpect(jsonPath("$.eixos[0].temas[0].tema").value("saude"));
    }

    /** Mesma regra das outras consultas: politico OU partido, nunca os dois. */
    @Test
    void recusaOsDoisIdentificadoresJuntos() throws Exception {
        when(service.materias(any())).thenThrow(new IllegalArgumentException(
                "Informe 'tituloEleitoral' (politico) ou 'nrPartido' (partido) -- "
                        + "exatamente um dos dois."));

        mockMvc.perform(get("/api/analise/materias")
                        .header(KEY_HEADER, KEY)
                        .param("tituloEleitoral", "123456789012")
                        .param("nrPartido", "13"))
                .andExpect(status().isBadRequest());
    }
}
