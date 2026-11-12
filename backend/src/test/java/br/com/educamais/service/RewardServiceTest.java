package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.port.ClockPort;
import br.com.educamais.domain.port.GamificationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

/** RF29, RF30 / RN03: idempotencia da recompensa e ofensiva. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RewardServiceTest {

    @Mock
    private GamificationRepository gamificationRepository;

    private RewardService service;
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ClockPort clock = new ClockPort() {
            @Override
            public java.time.Instant now() {
                return java.time.Instant.parse("2026-11-12T10:00:00Z");
            }

            @Override
            public LocalDate today() {
                return LocalDate.of(2026, 11, 12);
            }
        };
        service = new RewardService(gamificationRepository, clock, new AttendanceMetrics(new SimpleMeterRegistry()));
        when(gamificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Mission finishedMission() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"),
                new MissionPlan.PlannedStep("B", "e", "y")), PlanSource.LLM);
        Mission mission = Mission.plan(UUID.randomUUID(), studentId, UUID.randomUUID(), plan);
        mission.markNotified();
        mission.start();
        mission.completeCurrentStep();
        mission.completeCurrentStep();
        return mission;
    }

    @Test
    @DisplayName("Missao concluida concede XP e diamante")
    void concedeRecompensa() {
        when(gamificationRepository.findByStudentId(studentId))
                .thenReturn(Optional.of(GamificationProfile.newFor(studentId)));
        when(gamificationRepository.existsRewardByKey(any())).thenReturn(false);

        RewardService.RewardGrant grant = service.grantMissionReward(finishedMission(), "evt-1");

        assertThat(grant.newlyGranted()).isTrue();
        assertThat(grant.profile().totalXp()).isEqualTo(50);
        assertThat(grant.profile().diamonds()).isEqualTo(1);
        assertThat(grant.profile().currentStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("Recompensa nao e duplicada em reprocessamento (RN03)")
    void recompensaIdempotente() {
        when(gamificationRepository.findByStudentId(studentId))
                .thenReturn(Optional.of(GamificationProfile.newFor(studentId)));
        when(gamificationRepository.existsRewardByKey(any())).thenReturn(true);

        RewardService.RewardGrant grant = service.grantMissionReward(finishedMission(), "evt-1");

        assertThat(grant.newlyGranted()).isFalse();
        assertThat(grant.profile().totalXp()).isZero();
        verify(gamificationRepository, never()).saveReward(any());
    }

    @Test
    @DisplayName("Missao em andamento nao concede recompensa")
    void missaoIncompleta() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"),
                new MissionPlan.PlannedStep("B", "e", "y")), PlanSource.LLM);
        Mission running = Mission.plan(UUID.randomUUID(), studentId, UUID.randomUUID(), plan);
        running.markNotified();
        running.start();

        assertThatThrownBy(() -> service.grantMissionReward(running, "evt-2"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("conclusao");
    }
}
