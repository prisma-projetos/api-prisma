package br.com.prisma.api_prisma.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.PoliticoDetalheResponse;
import br.com.prisma.api_prisma.dto.PoliticoResponse;
import br.com.prisma.api_prisma.service.CatalogoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** O identificador do politico vai no corpo da requisicao, nao no path. */
@RestController
@RequestMapping("/api/politicos")
@Tag(name = "Politicos", description = "Consulta de politicos e do historico de candidaturas")
public class PoliticoController {

    private final CatalogoQueryService service;

    public PoliticoController(CatalogoQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista politicos",
            description = "Busca por nome ou nome social, paginada.")
    public Page<PoliticoResponse> listar(
            @Parameter(description = "Trecho do nome") @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listarPoliticos(q, pagina, tamanho);
    }

    @GetMapping("/detalhe")
    @Operation(summary = "Detalha um politico",
            description = "Dados pessoais e historico de candidaturas. Cargo, UF e partido "
                    + "vivem na candidatura, nao na pessoa: mudam de uma eleicao para outra.")
    public PoliticoDetalheResponse detalhe(@RequestBody ConsultaRequest request) {
        return service.detalharPolitico(request.tituloEleitoral());
    }
}
