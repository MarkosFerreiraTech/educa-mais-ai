package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.event.MissionCompletedEvent;
import br.com.educamais.domain.exception.AccessDeniedException;
import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.port.MissionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/** RF27, RF28: autorizacao por dono, inicio de missao e evento de conclusao. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MissionService service;
    private final UUID missionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MissionService(missionRepository, eventPublisher);
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Mission mission() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes", "Explicacao", "Exemplo"),
                new MissionPlan.PlannedStep("Reta numerica", "Explicacao", "Exemplo")), PlanSource.LLM);
        return Mission.plan(missionId, studentId, UUID.randomUUID(), plan);
    }

    @Test
    @DisplayName("aluno acessa apenas a propria missao")
    void acessoNegadoParaOutroAluno() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission()));

        assertThatThrownBy(() -> service.getForStudent(missionId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("missao inexistente gera 404")
    void missaoInexistente() {
        when(missionRepository.findById(missionId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getForStudent(missionId, studentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("iniciar transiciona PLANIFIED -> NOTIFIED -> IN_PROGRESS e persiste")
    void iniciaMissao() {
        Mission mission = mission();
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));

        Mission started = service.start(missionId, studentId);

        assertThat(started.status()).isEqualTo(MissionStatus.IN_PROGRESS);
        verify(missionRepository).save(mission);
    }

    @Test
    @DisplayName("missao cancelada nao pode ser iniciada")
    void missaoCancelada() {
        Mission mission = mission();
        mission.markNotified();
        mission.cancel();
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));

        assertThatThrownBy(() -> service.start(missionId, studentId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("iniciada");
    }

    @Test
    @DisplayName("missao ja concluida nao e persistida novamente")
    void missaoConcluida() {
        Mission mission = mission();
        mission.markNotified();
        mission.start();
        mission.completeCurrentStep();
        mission.completeCurrentStep();
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));

        service.start(missionId, studentId);

        verify(missionRepository, never()).save(any());
    }

    @Test
    @DisplayName("conclusao publica MissionCompletedEvent (Observer)")
    void publicaEventoDeConclusao() {
        Mission mission = mission();
        mission.markNotified();
        mission.start();
        mission.completeCurrentStep();
        mission.completeCurrentStep();

        service.save(mission);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(MissionCompletedEvent.class, event -> {
            assertThat(event.missionId()).isEqualTo(missionId);
            assertThat(event.withSupport()).isFalse();
        });
    }

    @Test
    @DisplayName("missao em andamento nao publica evento")
    void semEventoEmAndamento() {
        Mission mission = mission();
        mission.markNotified();
        mission.start();

        service.save(mission);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("passo atual ausente gera erro de regra")
    void semPassoAtual() {
        Mission mission = mission();
        mission.markNotified();
        mission.start();
        mission.completeCurrentStep();
        mission.completeCurrentStep();

        assertThatThrownBy(() -> service.currentStep(mission)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("lista e busca simples delegam ao repositorio")
    void consultasDelegadas() {
        Mission mission = mission();
        when(missionRepository.findByStudentId(studentId)).thenReturn(List.of(mission));
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));

        assertThat(service.listForStudent(studentId)).containsExactly(mission);
        assertThat(service.get(missionId)).isSameAs(mission);
    }
}
