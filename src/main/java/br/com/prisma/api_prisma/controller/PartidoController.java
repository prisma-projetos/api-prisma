package br.com.prisma.api_prisma.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.PartidoResponse;
import br.com.prisma.api_prisma.service.CatalogoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** O numero do partido vai no corpo da requisicao, nao no path. */
@RestController
@RequestMapping("/api/partidos")
@Tag(name = "Partidos", description = "Consulta de partidos politicos")
public class PartidoController {

    private final CatalogoQueryService service;

    public PartidoController(CatalogoQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista partidos", description = "Busca por sigla ou nome.")
    public List<PartidoResponse> listar(
            @Parameter(description = "Trecho da sigla ou do nome")
            @RequestParam(required = false) String q) {
        return service.listarPartidos(q);
    }

    @GetMapping("/detalhe")
    @Operation(summary = "Detalha um partido")
    public PartidoResponse detalhe(@RequestBody ConsultaRequest request) {
        return service.detalharPartido(request.nrPartido());
    }
}
