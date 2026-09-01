package br.com.prisma.api_prisma.security;

/**
 * Constantes da camada de seguranca.
 *
 * <p>A chave em si NAO fica aqui. Ela e lida de {@code api.security.key}
 * diretamente por {@link ApiKeyFilter}, que e o unico ponto que precisa
 * compara-la. Guardar uma segunda copia em um bean so aumentaria a superficie
 * de exposicao -- em heap dump, em log de bean, em serializacao acidental --
 * sem nenhum ganho.
 */
public final class SecurityConstants {

    /** Header no qual a chave de API deve ser enviada. */
    public static final String API_KEY_HEADER = "X-API-KEY";

    /** Rotas liberadas sem chave de API (documentacao do Swagger / OpenAPI). */
    public static final String[] PUBLIC_PATHS = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private SecurityConstants() {
    }
}
