package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.MissionRepository;
import br.com.educamais.domain.port.ReportRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** RF34: indicadores da turma. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherDashboardServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private MissionRepository missionRepository;
    @Mock
    private ReportRepository reportRepository;

    @Test
    @DisplayName("Dashboard agrega pendentes, concluidas e taxa de conclusao")
    void dashboard() {
        UUID classroomId = UUID.randomUUID();
        Student ana = Student.register("Ana Souza", "ana@escola.br", "+5581999998888", null, "hash", null,
                classroomId, true);
        Student bia = Student.register("Bia Lima", "bia@escola.br", null, null, "hash", null, classroomId, false);
        when(studentRepository.findByClassroomId(classroomId)).thenReturn(List.of(ana, bia));

        Mission pending = Mission.plan(UUID.randomUUID(), ana.id(), UUID.randomUUID(), new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"), new MissionPlan.PlannedStep("B", "e", "y")),
                PlanSource.LLM));
        pending.markNotified();
        Mission finished = Mission.plan(UUID.randomUUID(), bia.id(), UUID.randomUUID(), new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"), new MissionPlan.PlannedStep("B", "e", "y")),
                PlanSource.LLM));
        finished.markNotified();
        finished.start();
        finished.completeCurrentStep();
        finished.completeCurrentStep();

        when(missionRepository.findByStudentId(ana.id())).thenReturn(List.of(pending));
        when(missionRepository.findByStudentId(bia.id())).thenReturn(List.of(finished));
        when(reportRepository.findByStudentId(any())).thenReturn(List.of());

        TeacherDashboardService.ClassroomDashboard dashboard =
                new TeacherDashboardService(studentRepository, missionRepository, reportRepository)
                        .dashboard(classroomId);

        assertThat(dashboard.totalStudents()).isEqualTo(2);
        assertThat(dashboard.pendingMissions()).isEqualTo(1);
        assertThat(dashboard.completedMissions()).isEqualTo(1);
        assertThat(dashboard.completionRate()).isEqualTo(50);
        assertThat(dashboard.reachableByWhatsApp()).isEqualTo(1);
    }

    @Test
    @DisplayName("Turma sem missões retorna taxa zero")
    void turmaVazia() {
        UUID classroomId = UUID.randomUUID();
        when(studentRepository.findByClassroomId(classroomId)).thenReturn(List.of());

        TeacherDashboardService.ClassroomDashboard dashboard =
                new TeacherDashboardService(studentRepository, missionRepository, reportRepository)
                        .dashboard(classroomId);

        assertThat(dashboard.completionRate()).isZero();
        assertThat(dashboard.students()).isEmpty();
    }
}
