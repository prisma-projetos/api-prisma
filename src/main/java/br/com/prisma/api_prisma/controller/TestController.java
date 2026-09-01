package br.com.prisma.api_prisma.controller;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Test", description = "Endpoint de verificacao da API e da chave configurada")
public class TestController {

    @GetMapping("/test")
    @Operation(summary = "Confirma que a API esta no ar e a chave e valida")
    public Map<String, Object> test() {
        return Map.of(
                "status", "ok",
                "application", "api-prisma",
                "timestamp", OffsetDateTime.now().toString());
    }
}
