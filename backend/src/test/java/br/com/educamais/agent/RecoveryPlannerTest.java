package br.com.educamais.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.educamais.agent.planner.FallbackPlanner;
import br.com.educamais.agent.planner.PromptFactory;
import br.com.educamais.agent.planner.RecoveryPlanRequest;
import br.com.educamais.agent.planner.RecoveryPlanner;
import br.com.educamais.agent.validation.GroundedContentValidator;
import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.Syllabus;
import br.com.educamais.domain.port.LlmPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** RF12-RF15, RF21: planejamento, grounding e fallback. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecoveryPlannerTest {

    @Mock
    private LlmPort llmPort;

    private RecoveryPlanner planner;
    private RecoveryPlanRequest request;

    @BeforeEach
    void setUp() {
        planner = new RecoveryPlanner(llmPort, new PromptFactory(), new GroundedContentValidator(),
                new FallbackPlanner(), 5);

        Student student = Student.register("Ana Souza", "ana@escola.br", "+5581999998888", null, "hash",
                null, null, true);
        student.defineInterests(List.of(Interest.of(InterestCode.FOOTBALL, 5)));
        Syllabus syllabus = new Syllabus(
                List.of("Comparar fracoes com denominadores diferentes", "Representar fracoes na reta numerica"),
                List.of("fracoes equivalentes", "comparacao de fracoes"),
                List.of("fracoes com mesmo denominador"), "BNCC EF06MA07");
        Lesson lesson = new Lesson(UUID.randomUUID(), UUID.randomUUID(), "Matematica", "Fracoes equivalentes",
                LocalDate.now(), syllabus);
        request = new RecoveryPlanRequest(student, lesson, InterestCode.FOOTBALL, "evt-plan");
    }

    @Test
    @DisplayName("Plano do LLM ancorado na ementa e aceito")
    void planoDoLlmAceito() {
        MissionPlan llmPlan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes com denominadores diferentes",
                        "Explicacao com futebol", "2/4 de gols"),
                new MissionPlan.PlannedStep("Representar fracoes na reta numerica",
                        "Reta numerica com fracoes", "Marcar 1/4")), PlanSource.LLM);
        when(llmPort.planMission(any())).thenReturn(Optional.of(llmPlan));

        RecoveryPlanner.PlanningResult result = planner.plan(request);

        assertThat(result.source()).isEqualTo(PlanSource.LLM);
        assertThat(result.plan().steps()).hasSize(2);
        assertThat(result.discardedObjectives()).isEmpty();
    }

    @Test
    @DisplayName("Passos fora da ementa sao descartados e o plano invalido aciona o fallback (RF15, RF21)")
    void groundingDescartaPassosInvalidos() {
        MissionPlan llmPlan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Numeros complexos e imaginarios", "Conteudo avancado", "z = a+bi"),
                new MissionPlan.PlannedStep("Matrizes e determinantes", "Algebra linear", "det = 0")), PlanSource.LLM);
        when(llmPort.planMission(any())).thenReturn(Optional.of(llmPlan));

        RecoveryPlanner.PlanningResult result = planner.plan(request);

        assertThat(result.source()).isEqualTo(PlanSource.FALLBACK);
        assertThat(result.plan().isValid()).isTrue();
        assertThat(result.plan().steps()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Plano parcialmente descartado e complementado pelo fallback ancorado na ementa")
    void planoParcialUsaFallback() {
        MissionPlan llmPlan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes equivalentes", "Fracoes na pratica", "2/4 = 1/2"),
                new MissionPlan.PlannedStep("Estudar matrizes e determinantes", "Algebra linear", "det = 0")),
                PlanSource.LLM);
        when(llmPort.planMission(any())).thenReturn(Optional.of(llmPlan));

        RecoveryPlanner.PlanningResult result = planner.plan(request);

        assertThat(result.discardedObjectives()).hasSize(1);
        assertThat(result.discardedObjectives().get(0)).contains("matrizes");

        MissionPlan complemented = planner.fallback(request);
        assertThat(complemented.isValid()).isTrue();
        assertThat(complemented.steps().get(0).objective()).contains("Comparar fracoes");
    }

    @Test
    @DisplayName("LLM indisponivel aciona fallback deterministico com a ementa (RF21)")
    void fallbackQuandoLlmIndisponivel() {
        when(llmPort.planMission(any())).thenReturn(Optional.empty());

        RecoveryPlanner.PlanningResult result = planner.plan(request);

        assertThat(result.source()).isEqualTo(PlanSource.FALLBACK);
        assertThat(result.plan().steps()).hasSize(2);
        assertThat(result.plan().steps().get(0).objective()).contains("Comparar fracoes");
        assertThat(result.plan().steps().get(0).explanation()).contains("futebol");
    }

    @Test
    @DisplayName("Plano do LLM sem passos suficientes cai no fallback")
    void planoIncompleto() {
        when(llmPort.planMission(any())).thenReturn(Optional.of(new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes", "Explicacao", "Exemplo")), PlanSource.LLM)));

        RecoveryPlanner.PlanningResult result = planner.plan(request);

        assertThat(result.source()).isEqualTo(PlanSource.FALLBACK);
    }
}
