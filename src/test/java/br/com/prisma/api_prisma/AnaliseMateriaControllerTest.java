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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.prisma.api_prisma.controller.AnaliseMateriaController;
import br.com.prisma.api_prisma.dto.AnaliseMateriaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorEixoResponse;
import br.com.prisma.api_prisma.dto.AnalisePorEixoResponse.EixoContagem;
import br.com.prisma.api_prisma.dto.Proveniencia;
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrPartido\":13}"))
                .andExpect(status().isUnauthorized());
    }

    /** Titulo e resumo podem ser gerados: nunca circulam sem o metodo. */
    @Test
    void materiaTrazSempreOMetodoQueAProduziu() throws Exception {
        var materia = new AnaliseMateriaResponse(
                1, "saude", "Prefeitura anuncia novo hospital",
                "Texto resumido da materia.", "https://exemplo.com/noticia",
                7, "Diario do Vale", null, "analyser-baseline-v1", "v1", 4242L);

        when(service.materias(any()))
                .thenReturn(new PageImpl<>(List.of(materia), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/analise/materias")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrPartido\":13}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eixo").value("saude"))
                .andExpect(jsonPath("$.content[0].metodo").value("analyser-baseline-v1"))
                .andExpect(jsonPath("$.content[0].pipelineVersion").value("v1"));
    }

    /** Materia sem eixo e informacao, nao lixo: aparece como grupo proprio. */
    @Test
    void porEixoExpoeAsMateriasSemEixo() throws Exception {
        when(service.porEixo(any())).thenReturn(new AnalisePorEixoResponse(
                "partido", "13", "PT", 10, 4,
                List.of(new EixoContagem("saude", 6, 60.0),
                        new EixoContagem("(eixo nao atribuido)", 4, 40.0)),
                List.of(new Proveniencia("analyser-baseline-v1", "v1", 10))));

        mockMvc.perform(get("/api/analise/por-eixo")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrPartido\":13}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMaterias").value(10))
                .andExpect(jsonPath("$.semEixo").value(4))
                .andExpect(jsonPath("$.eixos[0].eixo").value("saude"))
                .andExpect(jsonPath("$.proveniencia[0].metodo").value("analyser-baseline-v1"));
    }

    /** Mesma regra das outras consultas: politico OU partido, nunca os dois. */
    @Test
    void recusaOsDoisIdentificadoresJuntos() throws Exception {
        when(service.materias(any())).thenThrow(new IllegalArgumentException(
                "Informe 'tituloEleitoral' (politico) ou 'nrPartido' (partido) -- "
                        + "exatamente um dos dois."));

        mockMvc.perform(get("/api/analise/materias")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloEleitoral\":\"123456789012\",\"nrPartido\":13}"))
                .andExpect(status().isBadRequest());
    }
}
