package br.com.prisma.api_prisma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.com.prisma.api_prisma.controller.TestController;
import br.com.prisma.api_prisma.security.ApiKeyFilter;
import br.com.prisma.api_prisma.security.SecurityConfig;

/**
 * Fatia web: sobe apenas o controller e a configuracao de seguranca,
 * sem datasource nem JPA.
 */
@WebMvcTest(TestController.class)
@Import({ SecurityConfig.class, ApiKeyFilter.class })
@TestPropertySource(properties = "api.security.key=chave-de-teste")
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveResponderOkComChaveValida() throws Exception {
        mockMvc.perform(get("/api/test").header("X-API-KEY", "chave-de-teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.application").value("api-prisma"));
    }

    @Test
    void deveRecusarSemChave() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarComChaveErrada() throws Exception {
        mockMvc.perform(get("/api/test").header("X-API-KEY", "chave-errada"))
                .andExpect(status().isUnauthorized());
    }
}
