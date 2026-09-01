package br.com.prisma.api_prisma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import br.com.prisma.api_prisma.security.ApiKeyFilter;

/**
 * A configuracao da chave e validada no boot, nao na primeira requisicao: uma
 * chave ausente ou fraca subiria a aplicacao aparentemente funcionando, com a
 * API inteira efetivamente aberta.
 */
class ApiKeyFilterTest {

    /** Valor sintetico, apenas comprido o bastante. Nunca uma chave real. */
    private static final String CHAVE_FORTE = "x".repeat(32);

    @Test
    void recusaChaveAusente() {
        assertThatThrownBy(() -> new ApiKeyFilter(null, ambiente("dev")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nao configurada");
    }

    @Test
    void recusaChaveVazia() {
        assertThatThrownBy(() -> new ApiKeyFilter("   ", ambiente("dev")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nao configurada");
    }

    @Test
    void recusaChaveCurtaForaDoLocal() {
        assertThatThrownBy(() -> new ApiKeyFilter("curta123", ambiente("dev")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("16 caracteres");

        assertThatThrownBy(() -> new ApiKeyFilter("curta123", ambiente("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("16 caracteres");
    }

    /** O profile local usa chave fixa curta de proposito, contra banco descartavel. */
    @Test
    void aceitaChaveCurtaApenasNoLocal() {
        assertThatCode(() -> new ApiKeyFilter("123456", ambiente("local")))
                .doesNotThrowAnyException();
    }

    @Test
    void aceitaChaveForteEmQualquerAmbiente() {
        assertThatCode(() -> new ApiKeyFilter(CHAVE_FORTE, ambiente("prod")))
                .doesNotThrowAnyException();
        assertThat(CHAVE_FORTE.length()).isGreaterThanOrEqualTo(16);
    }

    private static MockEnvironment ambiente(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return environment;
    }
}
