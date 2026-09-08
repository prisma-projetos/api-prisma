package br.com.prisma.api_prisma.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prisma.api_prisma.dto.AnaliseMateriaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorEixoResponse;
import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.service.AnaliseMateriaQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Analise no nivel da materia: eixo, titulo e resumo.
 *
 * <p>Separado de {@link AnaliseController}, que agrega sentimento por
 * paragrafo. Os dois vivem sob {@code /api/analise} mas leem tabelas
 * diferentes, escritas por componentes diferentes.
 */
@RestController
@RequestMapping("/api/analise")
@Tag(name = "Analise por materia",
        description = "Materias analisadas, com eixo tematico e resumo")
public class AnaliseMateriaController {

    private final AnaliseMateriaQueryService service;

    public AnaliseMateriaController(AnaliseMateriaQueryService service) {
        this.service = service;
    }

    @GetMapping("/materias")
    @Operation(summary = "Materias analisadas sobre a entidade",
            description = "A unidade e a materia, nao o paragrafo. Aceita filtro por eixo "
                    + "e por periodo. Titulo e resumo podem ser saida de modelo -- a "
                    + "resposta sempre diz qual metodo os produziu.")
    public Page<AnaliseMateriaResponse> materias(@RequestBody ConsultaRequest request) {
        return service.materias(request);
    }

    @GetMapping("/por-eixo")
    @Operation(summary = "Materias abertas por eixo tematico",
            description = "Expoe concentracao de pauta: se quase toda a cobertura cai num "
                    + "unico eixo, o agregado descreve aquele recorte, nao a cobertura "
                    + "em geral. Materias sem eixo atribuido aparecem como grupo proprio.")
    public AnalisePorEixoResponse porEixo(@RequestBody ConsultaRequest request) {
        return service.porEixo(request);
    }
}
