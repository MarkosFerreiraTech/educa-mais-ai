package br.com.educamais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.enums.StepStatus;
import br.com.educamais.domain.exception.InvalidMissionTransitionException;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** RN02, RN05, RF13, RF18, RF19: ciclo de vida da missao. */
class MissionTest {

    private Mission newMission() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes", "Explicacao 1", "Exemplo 1"),
                new MissionPlan.PlannedStep("Representar na reta", "Explicacao 2", "Exemplo 2")),
                PlanSource.LLM);
        return Mission.plan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), plan);
    }

    @Test
    @DisplayName("Plano valido exige de 2 a 5 passos")
    void planoValido() {
        Mission mission = newMission();
        assertThat(mission.steps()).hasSize(2);
        assertThat(mission.status()).isEqualTo(MissionStatus.PLANNED);
        assertThat(mission.progressPercent()).isZero();
    }

    @Test
    @DisplayName("Plano com menos de 2 passos e invalido")
    void planoInvalido() {
        MissionPlan plan = new MissionPlan(List.of(new MissionPlan.PlannedStep("Unico", "e", "x")), PlanSource.LLM);
        assertThat(plan.isValid()).isFalse();
    }

    @Test
    @DisplayName("Cancelamento apos notificacao e permitido")
    void cancelamentoPermitido() {
        Mission mission = newMission();
        mission.markNotified();
        mission.cancel();
        assertThat(mission.status()).isEqualTo(MissionStatus.CANCELLED);
        assertThatThrownBy(mission::markNotified).isInstanceOf(InvalidMissionTransitionException.class);
    }

    @Test
    @DisplayName("Missao planejada nao inicia duas vezes e conclui corretamente apos start")
    void inicioDaMissao() {
        Mission mission = newMission();
        mission.markNotified();
        mission.start();
        assertThat(mission.status()).isEqualTo(MissionStatus.IN_PROGRESS);
        assertThat(mission.currentStep().orElseThrow().status()).isEqualTo(StepStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Acertos concluem todos os passos e finalizam a missao")
    void concluiMissao() {
        Mission mission = newMission();
        mission.markNotified();
        mission.start();
        mission.completeCurrentStep();
        assertThat(mission.status()).isEqualTo(MissionStatus.IN_PROGRESS);
        assertThat(mission.progressPercent()).isEqualTo(50);
        mission.completeCurrentStep();
        assertThat(mission.status()).isEqualTo(MissionStatus.COMPLETED);
        assertThat(mission.completedAt()).isNotNull();
        assertThat(mission.progressPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("Passo com erro escala a estrategia pedagogica (RF18)")
    void escadaDeEstrategias() {
        Mission mission = newMission();
        mission.markNotified();
        mission.start();
        assertThat(mission.currentStep().orElseThrow().strategy()).isEqualTo(PedagogicalStrategyType.EXPLICIT);

        mission.adaptCurrentStep();
        assertThat(mission.currentStep().orElseThrow().strategy()).isEqualTo(PedagogicalStrategyType.ANALOGY);
        mission.adaptCurrentStep();
        assertThat(mission.currentStep().orElseThrow().strategy()).isEqualTo(PedagogicalStrategyType.VISUAL);
        mission.adaptCurrentStep();
        assertThat(mission.currentStep().orElseThrow().strategy()).isEqualTo(PedagogicalStrategyType.STEP_BY_STEP);
        assertThat(mission.currentStep().orElseThrow().status()).isEqualTo(StepStatus.NEEDS_REINFORCEMENT);
    }

    @Test
    @DisplayName("Missao com reforco finaliza como COMPLETED_WITH_SUPPORT")
    void conclusaoComApoio() {
        Mission mission = newMission();
        mission.markNotified();
        mission.start();
        mission.adaptCurrentStep();
        mission.completeCurrentStep();
        mission.completeCurrentStep();
        assertThat(mission.status()).isEqualTo(MissionStatus.COMPLETED_WITH_SUPPORT);
        assertThat(mission.isFinished()).isTrue();
    }

    @Nested
    @DisplayName("MissionStep")
    class StepTest {

        @Test
        @DisplayName("Maximo de 3 tentativas por passo (RN05)")
        void limiteDeTentativas() {
            Mission mission = newMission();
            mission.start();
            mission.adaptCurrentStep();
            mission.adaptCurrentStep();
            mission.adaptCurrentStep();
            assertThat(mission.currentStep().orElseThrow().attempts()).isEqualTo(3);
            assertThat(mission.currentStep().orElseThrow().canRetry()).isFalse();
        }

        @Test
        @DisplayName("Nova explicacao substitui a anterior mantendo o objetivo")
        void atualizaExplicacao() {
            Mission mission = newMission();
            mission.start();
            String objective = mission.currentStep().orElseThrow().objective();
            mission.applyExplanation("Nova explicacao com futebol", "Novo exemplo");
            assertThat(mission.currentStep().orElseThrow().explanation()).contains("futebol");
            assertThat(mission.currentStep().orElseThrow().objective()).isEqualTo(objective);
        }
    }
}
