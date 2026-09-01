package br.com.prisma.api_prisma.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;

/**
 * API stateless: sem sessao, sem CSRF, sem form login.
 * Toda rota exige {@code X-API-KEY}, exceto a documentacao do OpenAPI.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;

    public SecurityConfig(ApiKeyFilter apiKeyFilter) {
        this.apiKeyFilter = apiKeyFilter;
    }

    /**
     * {@link ApiKeyFilter} e um {@code @Component}, e todo bean do tipo Filter e
     * registrado automaticamente no filter chain do servlet container. La ele
     * rodaria ANTES do Spring Security, cujo SecurityContextHolderFilter em
     * seguida limparia o contexto (a API e stateless) -- e o guard do
     * OncePerRequestFilter impediria a segunda execucao, ja dentro da cadeia de
     * seguranca. O resultado seria 403 mesmo com chave valida. Este bean desliga
     * esse registro automatico: o filtro roda apenas onde foi posicionado abaixo.
     */
    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(ApiKeyFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Sem isto, o encaminhamento interno para /error tambem cai em
                        // anyRequest().authenticated() e QUALQUER excecao vira 403,
                        // escondendo o 400/404/500 real. O filtro de API Key nao roda
                        // neste dispatch, entao a rota nao fica exposta.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC)
                        .permitAll()
                        .requestMatchers(SecurityConstants.PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers
                        // Uma vez servida em HTTPS, o navegador nunca mais tenta HTTP --
                        // e o que impede a chave de API vazar em um downgrade de conexao.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // A URL pode conter termo de busca; nao vaza-la para terceiros.
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicy.NO_REFERRER)))
                // Sem Content-Security-Policy aqui de proposito: o header vale para
                // toda resposta, e um 'default-src none' quebraria o Swagger UI, que e
                // servido pela propria aplicacao. Para uma API JSON ele acrescenta
                // pouco -- nosniff e X-Frame-Options: DENY ja cobrem o risco real.
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
