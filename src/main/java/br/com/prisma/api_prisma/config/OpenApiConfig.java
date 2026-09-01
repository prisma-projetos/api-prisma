package br.com.prisma.api_prisma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Declara o esquema de API Key para o Swagger UI, de modo que o botao
 * "Authorize" envie o header {@code X-API-KEY} nas chamadas de teste.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "apiKey";

    @Bean
    OpenAPI prismaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Prisma")
                        .description("Consulta de conteudos classificados e sinteses "
                                + "agregadas por entidade politica e perspectiva.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
