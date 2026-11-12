package br.com.educamais.agent;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.agent.strategy.AdaptiveStrategySelector;
import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF18, RN05: selecao adaptativa de estrategia. */
class AdaptiveStrategySelectorTest {

    private final AdaptiveStrategySelector selector = new AdaptiveStrategySelector();

    private Mission mission() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"),
                new MissionPlan.PlannedStep("B", "e", "y")), PlanSource.LLM);
        Mission mission = Mission.plan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), plan);
        mission.markNotified();
        mission.start();
        return mission;
    }

    @Test
    @DisplayName("Sequencia EXPLICIT -> ANALOGY -> VISUAL -> STEP_BY_STEP")
    void sequenciaDeEscalonamento() {
        Mission mission = mission();
        assertThat(selector.nextStrategy(mission.currentStep().orElseThrow()))
                .isEqualTo(PedagogicalStrategyType.ANALOGY);
        mission.adaptCurrentStep();
        assertThat(selector.nextStrategy(mission.currentStep().orElseThrow()))
                .isEqualTo(PedagogicalStrategyType.VISUAL);
        mission.adaptCurrentStep();
        assertThat(selector.nextStrategy(mission.currentStep().orElseThrow()))
                .isEqualTo(PedagogicalStrategyType.STEP_BY_STEP);
    }

    @Test
    @DisplayName("Teto e STEP_BY_STEP")
    void tetoDeEstrategia() {
        assertThat(PedagogicalStrategyType.STEP_BY_STEP.escalate())
                .isEqualTo(PedagogicalStrategyType.STEP_BY_STEP);
        assertThat(selector.isReinforcement(PedagogicalStrategyType.STEP_BY_STEP)).isTrue();
        assertThat(selector.isReinforcement(PedagogicalStrategyType.ANALOGY)).isFalse();
    }
}
