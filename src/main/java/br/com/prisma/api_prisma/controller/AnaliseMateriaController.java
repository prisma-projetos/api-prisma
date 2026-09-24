package br.com.prisma.api_prisma.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prisma.api_prisma.dto.AnaliseMateriaResponse;
import br.com.prisma.api_prisma.dto.AnalisePorTemaResponse;
import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.dto.PanoramaResponse;
import br.com.prisma.api_prisma.service.AnaliseMateriaQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Analise no nivel da materia: resumo, tema e panorama do mes.
 *
 * <p>Separado de {@link AnaliseController}, que agrega sentimento por
 * paragrafo. Os dois vivem sob {@code /api/analise} mas leem tabelas
 * diferentes, escritas por componentes diferentes: aquele le
 * {@code tabela_classificacao} do classifier, este le {@code analyser_item} e
 * {@code analyser_panorama} do analyser.
 */
@RestController
@RequestMapping("/api/analise")
@Tag(name = "Analise por materia",
        description = "Materias resumidas, concentracao de pauta e panorama mensal")
public class AnaliseMateriaController {

    private final AnaliseMateriaQueryService service;

    public AnaliseMateriaController(AnaliseMateriaQueryService service) {
        this.service = service;
    }

    @GetMapping("/materias")
    @Operation(summary = "Materias analisadas sobre a entidade",
            description = "A unidade e a materia, nao o paragrafo. Aceita filtro por tema, "
                    + "por janela mensal e por periodo. O resumo e escrito por modelo -- a "
                    + "resposta sempre diz qual. Nao ha titulo: o analyser nao o persiste.")
    public Page<AnaliseMateriaResponse> materias(
            @ParameterObject @Valid ConsultaRequest consulta) {
        return service.materias(consulta);
    }

    @GetMapping("/por-tema")
    @Operation(summary = "Materias abertas por tema",
            description = "Expoe concentracao de pauta: se quase toda a cobertura cai num "
                    + "unico tema, o agregado descreve aquele recorte, nao a cobertura em "
                    + "geral. Materias sem tema atribuido aparecem como grupo proprio. "
                    + "Uma materia com varios temas conta em cada um.")
    public AnalisePorTemaResponse porTema(@ParameterObject @Valid ConsultaRequest consulta) {
        return service.porTema(consulta);
    }

    @GetMapping("/panorama")
    @Operation(summary = "Panorama consolidado da entidade no mes",
            description = "O texto que o analyser consolidou para cada eixo (polaridade) "
                    + "da janela, sempre na versao mais recente, com temas e fontes ja "
                    + "resolvidas para veiculo e link. Sem 'janela', responde o mes mais "
                    + "recente que tiver panorama.")
    public PanoramaResponse panorama(@ParameterObject @Valid ConsultaRequest consulta) {
        return service.panorama(consulta);
    }
}
