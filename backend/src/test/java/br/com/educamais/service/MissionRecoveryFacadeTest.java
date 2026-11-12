package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.agent.MissionOrchestrator;
import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Student;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** UC05 / RF27: fachada da jornada do aluno (iniciar, consultar exercicio, responder). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MissionRecoveryFacadeTest {

    @Mock
    private MissionService missionService;
    @Mock
    private StudentService studentService;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private MissionOrchestrator orchestrator;

    private MissionRecoveryFacade facade;
    private final UUID studentId = UUID.randomUUID();
    private final UUID missionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        facade = new MissionRecoveryFacade(missionService, studentService, assessmentService, orchestrator);
    }

    private static Mission mission(MissionStatus status) {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes", "Explicacao", "Exemplo"),
                new MissionPlan.PlannedStep("Reta numerica", "Explicacao", "Exemplo")), PlanSource.LLM);
        Mission mission = Mission.plan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), plan);
        if (status != MissionStatus.PLANNED) {
            mission.markNotified();
            mission.start();
        }
        return mission;
    }

    private Student student() {
        return Student.register("Ana Souza", "ana@escola.br", "+5581999998888", null, "hash", null, null, true);
    }

    @Test
    @DisplayName("iniciar devolve a jornada com progresso e primeiro passo")
    void iniciarMissao() {
        Mission mission = mission(MissionStatus.IN_PROGRESS);
        when(missionService.start(missionId, studentId)).thenReturn(mission);
        when(studentService.get(any())).thenReturn(student());

        MissionRecoveryFacade.MissionJourney journey = facade.start(missionId, studentId);

        assertThat(journey.mission()).isSameAs(mission);
        assertThat(journey.progress()).isZero();
        assertThat(journey.finished()).isFalse();
        assertThat(journey.feedback()).isNull();
    }

    @Test
    @DisplayName("consulta atual prepara o proximo exercicio do passo corrente")
    void consultaExercicio() {
        Mission mission = mission(MissionStatus.IN_PROGRESS);
        DiagnosticQuestion question = org.mockito.Mockito.mock(DiagnosticQuestion.class);
        when(missionService.getForStudent(missionId, studentId)).thenReturn(mission);
        when(assessmentService.prepareQuestion(mission)).thenReturn(question);
        when(studentService.get(any())).thenReturn(student());

        MissionRecoveryFacade.MissionJourney journey = facade.current(missionId, studentId);

        assertThat(journey.question()).isSameAs(question);
        verify(assessmentService).prepareQuestion(mission);
    }

    @Test
    @DisplayName("missao concluida nao prepara novo exercicio")
    void missaoConcluidaSemExercicio() {
        Mission mission = mission(MissionStatus.IN_PROGRESS);
        mission.completeCurrentStep();
        mission.completeCurrentStep();
        when(missionService.getForStudent(missionId, studentId)).thenReturn(mission);
        when(studentService.get(any())).thenReturn(student());

        MissionRecoveryFacade.MissionJourney journey = facade.current(missionId, studentId);

        assertThat(journey.finished()).isTrue();
        assertThat(journey.question()).isNull();
        assertThat(journey.progress()).isEqualTo(100);
        verify(assessmentService, never()).prepareQuestion(any());
    }

    @Test
    @DisplayName("resposta delega ao orquestrador e devolve feedback com a missao atualizada")
    void respondeExercicio() {
        Mission mission = mission(MissionStatus.IN_PROGRESS);
        UUID questionId = UUID.randomUUID();
        when(orchestrator.answer(missionId, studentId, questionId, "1/2")).thenReturn(
                new MissionOrchestrator.AnswerOutcome(mission, "CORRECT", "Muito bem!"));
        when(assessmentService.prepareQuestion(mission))
                .thenReturn(org.mockito.Mockito.mock(DiagnosticQuestion.class));
        when(studentService.get(any())).thenReturn(student());

        MissionRecoveryFacade.MissionJourney journey = facade.answer(missionId, studentId, questionId, "1/2");

        assertThat(journey.feedback()).isEqualTo("Muito bem!");
        assertThat(journey.classification()).isEqualTo("CORRECT");
        assertThat(journey.mission()).isSameAs(mission);
    }

    @Test
    @DisplayName("missao inexistente propaga erro de recurso")
    void missaoInexistente() {
        when(missionService.start(missionId, studentId))
                .thenThrow(new ResourceNotFoundException("Missao", missionId));

        assertThatThrownBy(() -> facade.start(missionId, studentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("lista de missoes do aluno e repassada")
    void listaMissoes() {
        List<Mission> missions = List.of(mission(MissionStatus.PLANNED));
        when(missionService.listForStudent(studentId)).thenReturn(missions);

        assertThat(facade.studentMissions(studentId)).isEqualTo(missions);
    }
}
