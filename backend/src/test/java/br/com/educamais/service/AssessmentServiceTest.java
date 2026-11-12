package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.educamais.agent.planner.PromptFactory;
import br.com.educamais.agent.strategy.AdaptiveStrategySelector;
import br.com.educamais.agent.strategy.AnalogyStrategy;
import br.com.educamais.agent.strategy.ExplicitStrategy;
import br.com.educamais.agent.strategy.PedagogicalStrategyCatalog;
import br.com.educamais.agent.strategy.StepByStepStrategy;
import br.com.educamais.agent.strategy.VisualStrategy;
import br.com.educamais.domain.enums.AnswerClassification;
import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.enums.QuestionType;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.DiagnosticRepository;
import br.com.educamais.domain.port.LlmPort;
import br.com.educamais.domain.port.StudentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

/** RF17, RF18: avaliacao e adaptacao pedagogica com fallback. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssessmentServiceTest {

    @Mock
    private DiagnosticRepository diagnosticRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private LlmPort llmPort;

    private AssessmentService service;
    private Mission mission;
    private DiagnosticQuestion question;

    @BeforeEach
    void setUp() {
        PedagogicalStrategyCatalog catalog = new PedagogicalStrategyCatalog(List.of(
                new ExplicitStrategy(), new AnalogyStrategy(), new VisualStrategy(), new StepByStepStrategy()));
        service = new AssessmentService(diagnosticRepository, studentRepository, null, new AdaptiveStrategySelector(),
                catalog, llmPort, new PromptFactory(), new AttendanceMetrics(new SimpleMeterRegistry()));

        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes", "Explicacao", "Exemplo"),
                new MissionPlan.PlannedStep("Reta numerica", "Explicacao", "Exemplo")), PlanSource.LLM);
        mission = Mission.plan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), plan);
        mission.markNotified();
        mission.start();

        question = new DiagnosticQuestion(null, mission.id(), mission.steps().get(0).id(), "2/4 equivale a?",
                QuestionType.MULTIPLE_CHOICE, List.of("1/2", "1/3"), "1/2", "2/4 simplifica para 1/2", 2);
        when(diagnosticRepository.findQuestionById(any())).thenReturn(Optional.of(question));
        when(diagnosticRepository.saveAttempt(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(llmPort.reExplain(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Resposta correta e classificada como CORRECT")
    void respostaCorreta() {
        AssessmentService.Evaluation evaluation = service.evaluate(question.id(), UUID.randomUUID(), "1/2",
                "futebol");
        assertThat(evaluation.classification()).isEqualTo(AnswerClassification.CORRECT);
        assertThat(evaluation.feedback()).contains("Isso mesmo");
    }

    @Test
    @DisplayName("Resposta incorreta e classificada como INCORRECT com feedback")
    void respostaIncorreta() {
        AssessmentService.Evaluation evaluation = service.evaluate(question.id(), UUID.randomUUID(), "1/3",
                "futebol");
        assertThat(evaluation.classification()).isEqualTo(AnswerClassification.INCORRECT);
        assertThat(evaluation.feedback()).contains("Quase la");
    }

    @Test
    @DisplayName("Erro escala a estrategia e aplica template quando o LLM nao responde (RF18)")
    void adaptacaoComFallback() {
        Student student = Student.register("Ana", "ana@escola.br", null, null, "hash", null, null, true);
        student.defineInterests(List.of(Interest.of(InterestCode.FOOTBALL, 5)));

        AssessmentService.Adaptation adaptation = service.adapt(mission, student, "1/3");

        assertThat(adaptation.strategy()).isEqualTo(PedagogicalStrategyType.ANALOGY);
        assertThat(adaptation.generatedByLlm()).isFalse();
        assertThat(adaptation.explanation()).isNotBlank();
        assertThat(mission.currentStep().orElseThrow().strategy()).isEqualTo(PedagogicalStrategyType.ANALOGY);
        assertThat(mission.currentStep().orElseThrow().attempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Terceira tentativa aplica reforco STEP_BY_STEP")
    void reforcoNaTerceiraTentativa() {
        Student student = Student.register("Ana", "ana@escola.br", null, null, "hash", null, null, true);
        service.adapt(mission, student, "errado");
        service.adapt(mission, student, "errado");
        AssessmentService.Adaptation third = service.adapt(mission, student, "errado");

        assertThat(third.strategy()).isEqualTo(PedagogicalStrategyType.STEP_BY_STEP);
        assertThat(third.reinforcement()).isTrue();
    }
}
