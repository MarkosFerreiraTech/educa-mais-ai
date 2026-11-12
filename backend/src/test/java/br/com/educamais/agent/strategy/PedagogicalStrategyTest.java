package br.com.educamais.agent.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** RF18: cada estrategia pedagogica (padrao Strategy) orienta o LLM e gera conteudo de reserva. */
class PedagogicalStrategyTest {

    private final List<PedagogicalStrategy> strategies = List.of(
            new ExplicitStrategy(), new AnalogyStrategy(), new VisualStrategy(), new StepByStepStrategy());

    private final PedagogicalStrategyCatalog catalog = new PedagogicalStrategyCatalog(strategies);

    @ParameterizedTest
    @EnumSource(PedagogicalStrategyType.class)
    @DisplayName("catalogo devolve a estrategia registrada para cada tipo")
    void catalogoCobreTodosOsTipos(PedagogicalStrategyType type) {
        PedagogicalStrategy strategy = catalog.get(type);
        assertThat(strategy.type()).isEqualTo(type);
        assertThat(strategy.instruction()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(PedagogicalStrategyType.class)
    @DisplayName("template explica o objetivo e contextualiza pelo interesse")
    void templatesSaoContextualizados(PedagogicalStrategyType type) {
        PedagogicalStrategy strategy = catalog.get(type);
        String explanation = strategy.templateExplanation("comparar fracoes");
        String example = strategy.templateExample("comparar fracoes", "futebol");

        assertThat(explanation).contains("comparar fracoes");
        assertThat(example).isNotBlank().containsIgnoringCase("futebol");
    }

    @Test
    @DisplayName("exemplo sem interesse informado nao quebra o template")
    void templateSemInteresse() {
        assertThat(catalog.get(PedagogicalStrategyType.VISUAL).templateExample("fracoes", null)).isNotBlank();
    }

    @Test
    @DisplayName("tipo nao registrado gera erro explicito")
    void tipoNaoRegistrado() {
        PedagogicalStrategyCatalog vazio = new PedagogicalStrategyCatalog(List.of());
        assertThatThrownBy(() -> vazio.get(PedagogicalStrategyType.ANALOGY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ANALOGY");
    }

    @Test
    @DisplayName("escada pedagogica termina em STEP_BY_STEP")
    void escadaCompleta() {
        assertThat(PedagogicalStrategyType.EXPLICIT.escalate()).isEqualTo(PedagogicalStrategyType.ANALOGY);
        assertThat(PedagogicalStrategyType.STEP_BY_STEP.escalate())
                .isEqualTo(PedagogicalStrategyType.STEP_BY_STEP);
    }
}
