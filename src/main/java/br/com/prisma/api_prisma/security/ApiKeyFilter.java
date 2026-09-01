package br.com.prisma.api_prisma.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Valida o header {@code X-API-KEY} contra a chave configurada em
 * {@code api.security.key}. Requisicoes das rotas publicas passam direto.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final byte[] expectedKey;

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    /** Comprimento minimo aceito fora do ambiente local. */
    private static final int TAMANHO_MINIMO = 16;

    public ApiKeyFilter(@Value("${api.security.key}") String apiKey, Environment environment) {
        // Falhar no boot, e nao na primeira requisicao: uma chave vazia subiria a
        // aplicacao "funcionando", com a API inteira efetivamente aberta.
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "api.security.key nao configurada. Defina API_SECURITY_KEY no ambiente.");
        }

        boolean local = environment.matchesProfiles("local");
        if (apiKey.length() < TAMANHO_MINIMO) {
            if (local) {
                // O profile local usa uma chave fixa e curta de proposito, contra um
                // banco descartavel. Fora dele, isso seria uma API aberta na pratica.
                log.warn("Chave de API curta ({} caracteres) -- aceitavel apenas no "
                        + "profile local.", apiKey.length());
            } else {
                throw new IllegalStateException(
                        "api.security.key tem menos de " + TAMANHO_MINIMO + " caracteres. "
                                + "Gere uma chave aleatoria longa para este ambiente.");
            }
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : SecurityConstants.PUBLIC_PATHS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String provided = request.getHeader(SecurityConstants.API_KEY_HEADER);

        if (provided == null || !matches(provided)) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\","
                            + "\"message\":\"Header " + SecurityConstants.API_KEY_HEADER
                            + " ausente ou invalido.\"}");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "api-key-client", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /** Comparacao em tempo constante, para nao vazar a chave por timing. */
    private boolean matches(String provided) {
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey);
    }
}
