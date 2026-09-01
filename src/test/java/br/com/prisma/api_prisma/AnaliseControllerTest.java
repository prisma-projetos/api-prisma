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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.prisma.api_prisma.controller.AnaliseController;
import br.com.prisma.api_prisma.dto.SentimentoPorMidiaResponse;
import br.com.prisma.api_prisma.dto.SentimentoPorMidiaResponse.MidiaSentimento;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse.ContagemSentimento;
import br.com.prisma.api_prisma.dto.SentimentoResumoResponse.Proveniencia;
import br.com.prisma.api_prisma.exception.NotFoundException;
import br.com.prisma.api_prisma.security.ApiKeyFilter;
import br.com.prisma.api_prisma.security.SecurityConfig;
import br.com.prisma.api_prisma.service.AnaliseQueryService;

@WebMvcTest(AnaliseController.class)
@Import({ SecurityConfig.class, ApiKeyFilter.class })
@TestPropertySource(properties = "api.security.key=chave-de-teste")
class AnaliseControllerTest {

    private static final String KEY_HEADER = "X-API-KEY";
    private static final String KEY = "chave-de-teste";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnaliseQueryService service;

    @Test
    void exigeChaveDeApi() throws Exception {
        mockMvc.perform(get("/api/analise/sentimento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrPartido\":13}"))
                .andExpect(status().isUnauthorized());
    }

    /** O numero nunca deve ser lido sem o modelo que o produziu. */
    @Test
    void resumoTrazSempreAProveniencia() throws Exception {
        when(service.resumoSentimento(any())).thenReturn(new SentimentoResumoResponse(
                "partido", "13", "PT", null, null, 3946,
                List.of(new ContagemSentimento("neutro", 3400, 86.16, 0.0)),
                List.of(new Proveniencia("lexicon-baseline-v1", "v1", 3946))));

        mockMvc.perform(get("/api/analise/sentimento")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrPartido\":13}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParagrafos").value(3946))
                .andExpect(jsonPath("$.proveniencia[0].metodo").value("lexicon-baseline-v1"))
                .andExpect(jsonPath("$.proveniencia[0].pipelineVersion").value("v1"));
    }

    /**
     * Dois tercos das classificacoes nao tem veiculo. Elas precisam aparecer
     * como grupo proprio: descarta-las faria o total parecer menor do que e.
     */
    @Test
    void recorteporMidiaExpoeParagrafosSemVeiculo() throws Exception {
        when(service.sentimentoPorMidia(any())).thenReturn(new SentimentoPorMidiaResponse(
                "politico", "122418060191", "Fulano", 1, 100, 60,
                List.of(new MidiaSentimento(1, "O Globo", 40, 40.0, 5, 30, 5),
                        new MidiaSentimento(null, "(veiculo nao identificado)",
                                60, 60.0, 10, 45, 5))));

        mockMvc.perform(get("/api/analise/por-midia")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloEleitoral\":\"122418060191\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParagrafos").value(100))
                .andExpect(jsonPath("$.paragrafosSemVeiculo").value(60))
                .andExpect(jsonPath("$.midias[1].idMidia").doesNotExist());
    }

    /** Politico XOR partido: e a mesma restricao que o banco impoe. */
    @Test
    void recusaOsDoisIdentificadoresJuntos() throws Exception {
        when(service.resumoSentimento(any()))
                .thenThrow(new IllegalArgumentException("exatamente um dos dois"));

        mockMvc.perform(get("/api/analise/sentimento")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloEleitoral\":\"122418060191\",\"nrPartido\":13}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void entidadeInexistenteRetorna404() throws Exception {
        when(service.resumoSentimento(any()))
                .thenThrow(new NotFoundException("Partido numero 999 nao encontrado."));

        mockMvc.perform(get("/api/analise/sentimento")
                        .header(KEY_HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nrPartido\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
