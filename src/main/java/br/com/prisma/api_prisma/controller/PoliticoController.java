package br.com.prisma.api_prisma.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.prisma.api_prisma.dto.CandidatoResumoResponse;
import br.com.prisma.api_prisma.dto.PoliticoDetalheResponse;
import br.com.prisma.api_prisma.service.CatalogoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** O titulo eleitoral vai na query string ({@code ?tituloEleitoral=}), nao no path. */
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
            description = "Busca por nome ou nome social, paginada. Um item por "
                    + "candidatura (nao por pessoa): sem anoEleicao, um mesmo "
                    + "politico aparece de novo por eleicao em que concorreu. "
                    + "Resumido para a listagem (tituloEleitoral, nome, cargo, "
                    + "urlFoto, nrPartido, siglaPartido -- o partido e o da "
                    + "candidatura); dados completos ficam em /detalhe.")
    public Page<CandidatoResumoResponse> listar(
            @Parameter(description = "Trecho do nome") @RequestParam(required = false) String q,
            @Parameter(description = "Filtra por numero de legenda do partido")
            @RequestParam(required = false) Integer nrPartido,
            @Parameter(description = "Filtra por ano da eleicao, ex. 2026. Sem "
                    + "isso, politicos com mais de uma candidatura aparecem "
                    + "repetidos, uma vez por eleicao")
            @RequestParam(required = false) Integer anoEleicao,
            @Parameter(description = "Filtra pelo cargo da candidatura, ex. "
                    + "PRESIDENTE, SENADOR, DEPUTADO FEDERAL. Sem diferenciar "
                    + "maiusculas de minusculas", example = "PRESIDENTE")
            @RequestParam(required = false) String cargo,
            @Parameter(description = "Filtra pela UF da candidatura, ex. RO. Sem "
                    + "diferenciar maiusculas de minusculas", example = "RO")
            @RequestParam(required = false) String uf,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listarPoliticos(q, nrPartido, anoEleicao, cargo, uf, pagina, tamanho);
    }

    @GetMapping("/detalhe")
    @Operation(summary = "Detalha um politico",
            description = "Dados pessoais e historico de candidaturas. Cargo, UF e partido "
                    + "vivem na candidatura, nao na pessoa: mudam de uma eleicao para outra.")
    public PoliticoDetalheResponse detalhe(
            @Parameter(description = "Numero do titulo eleitoral do politico",
                    required = true, example = "123456789012")
            @RequestParam String tituloEleitoral) {
        return service.detalharPolitico(tituloEleitoral);
    }
}
