package br.com.prisma.api_prisma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.prisma.api_prisma.dto.ConsultaRequest;
import br.com.prisma.api_prisma.model.Partido;
import br.com.prisma.api_prisma.repository.AnaliseMateriaRepository;
import br.com.prisma.api_prisma.repository.AnaliseMateriaRepository.ContagemPorEixo;

/**
 * A agregacao por eixo e a unica logica de verdade neste servico: ordenacao,
 * percentual e o tratamento do eixo nulo. O teste de controller mocka o
 * servico inteiro, entao nada disso era exercitado.
 */
class AnaliseMateriaQueryServiceTest {

    private AnaliseMateriaRepository analises;
    private AnaliseMateriaQueryService service;

    /** Consulta por partido, sem recorte de periodo nem de eixo. */
    private static final ConsultaRequest POR_PARTIDO =
            new ConsultaRequest(null, 13, null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        var catalogo = mock(CatalogoQueryService.class);
        var partido = mock(Partido.class);
        when(partido.getNumero()).thenReturn(13);
        when(partido.getSigla()).thenReturn("PT");
        when(catalogo.carregarPartido(anyInt())).thenReturn(partido);

        analises = mock(AnaliseMateriaRepository.class);
        when(analises.contarPorModelo(anyString(), anyInt())).thenReturn(List.of());

        service = new AnaliseMateriaQueryService(catalogo, analises);
    }

    @Test
    void ordenaEixosPorVolumeECalculaPercentual() {
        when(analises.contarPorEixo(anyString(), anyInt(), any(), any()))
                .thenReturn(List.of(eixo("educacao", 2), eixo("saude", 6), eixo("seguranca", 2)));

        var resposta = service.porEixo(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isEqualTo(10);
        assertThat(resposta.semEixo()).isZero();
        assertThat(resposta.eixos()).extracting("eixo")
                .containsExactly("saude", "educacao", "seguranca");
        assertThat(resposta.eixos().get(0).percentual()).isEqualTo(60.0);
        assertThat(resposta.eixos().get(1).percentual()).isEqualTo(20.0);
    }

    /** Materia sem eixo e informacao, nao lixo: vira grupo proprio, no fim. */
    @Test
    void materiasSemEixoViramGrupoProprioNoFim() {
        when(analises.contarPorEixo(anyString(), anyInt(), any(), any()))
                .thenReturn(List.of(eixo(null, 4), eixo("saude", 6)));

        var resposta = service.porEixo(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isEqualTo(10);
        assertThat(resposta.semEixo()).isEqualTo(4);
        assertThat(resposta.eixos()).extracting("eixo")
                .containsExactly("saude", "(eixo nao atribuido)");
        assertThat(resposta.eixos().get(1).total()).isEqualTo(4);
        assertThat(resposta.eixos().get(1).percentual()).isEqualTo(40.0);
    }

    /** Entidade sem nenhuma materia nao pode estourar divisao por zero. */
    @Test
    void semMateriasDevolveAgregadoVazio() {
        when(analises.contarPorEixo(anyString(), anyInt(), any(), any()))
                .thenReturn(List.of());

        var resposta = service.porEixo(POR_PARTIDO);

        assertThat(resposta.totalMaterias()).isZero();
        assertThat(resposta.semEixo()).isZero();
        assertThat(resposta.eixos()).isEmpty();
        assertThat(resposta.tipo()).isEqualTo("partido");
        assertThat(resposta.nome()).isEqualTo("PT");
    }

    /** Quando tudo cai num eixo so, o numero tem que dizer isso: 100%. */
    @Test
    void concentracaoTotalNumEixoAparecaComoCemPorCento() {
        when(analises.contarPorEixo(anyString(), anyInt(), any(), any()))
                .thenReturn(List.of(eixo("saude", 7)));

        var resposta = service.porEixo(POR_PARTIDO);

        assertThat(resposta.eixos()).singleElement()
                .satisfies(e -> assertThat(e.percentual()).isEqualTo(100.0));
    }

    /** Mesma regra das outras consultas: politico OU partido, nunca os dois. */
    @Test
    void recusaOsDoisIdentificadoresJuntos() {
        var ambos = new ConsultaRequest("123456789012", 13, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.porEixo(ambos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente um dos dois");
    }

    private static ContagemPorEixo eixo(String nome, long total) {
        return new ContagemPorEixo() {
            @Override
            public String getEixo() {
                return nome;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
