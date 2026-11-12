package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.educamais.agent.planner.PromptFactory;
import br.com.educamais.domain.model.AnswerSummary;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.Syllabus;
import br.com.educamais.domain.model.TeacherReport;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.LlmPort;
import br.com.educamais.domain.port.ReportRepository;
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

/** RF33: relatorio automatico com fallback de recomendacao. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LlmPort llmPort;

    private TeacherReportService service;
    private Mission mission;

    @BeforeEach
    void setUp() {
        service = new TeacherReportService(reportRepository, studentRepository, lessonRepository, llmPort,
                new PromptFactory(), new AttendanceMetrics(new SimpleMeterRegistry()));
        Student student = Student.register("Ana Souza", "ana@escola.br", null, null, "hash", null, null, true);
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"),
                new MissionPlan.PlannedStep("B", "e", "y")), br.com.educamais.domain.enums.PlanSource.LLM);
        mission = Mission.plan(UUID.randomUUID(), student.id(), UUID.randomUUID(), plan);
        mission.markNotified();
        mission.start();
        mission.completeCurrentStep();
        mission.completeCurrentStep();

        when(studentRepository.findById(student.id())).thenReturn(Optional.of(student));
        when(lessonRepository.findById(mission.lessonId())).thenReturn(Optional.of(new Lesson(mission.lessonId(),
                null, "Matematica", "Fracoes equivalentes", null, new Syllabus(List.of("Comparar fracoes"),
                List.of(), List.of(), "BNCC"))));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.findByMissionId(any())).thenReturn(Optional.empty());
        when(llmPort.recommendTeacherAction(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Relatorio e gerado com recomendacao por regra quando o LLM falha")
    void relatorioComFallback() {
        TeacherReport report = service.generate(mission, new AnswerSummary(2, 0, 0, List.of()));

        assertThat(report.id()).isNotNull();
        assertThat(report.successRate()).isEqualTo(100);
        assertThat(report.recommendation()).contains("autonomia");
        assertThat(report.deliveryStatus()).isEqualTo("DELIVERED");
        assertThat(report.summary()).contains("Fracoes equivalentes");
    }

    @Test
    @DisplayName("Relatorio existente nao e duplicado")
    void relatorioIdempotente() {
        TeacherReport existing = new TeacherReport(UUID.randomUUID(), mission.id(), mission.studentId(), null,
                "ja existe", List.of(), "ok", 80, "DELIVERED", null);
        when(reportRepository.findByMissionId(mission.id())).thenReturn(Optional.of(existing));

        TeacherReport report = service.generate(mission, AnswerSummary.empty());

        assertThat(report.summary()).isEqualTo("ja existe");
    }

    @Test
    @DisplayName("Recomendacao de lacuna persistente quando o acerto e baixo")
    void recomendacaoDeLacuna() {
        TeacherReport report = service.generate(mission, new AnswerSummary(1, 0, 3, List.of("Comparar fracoes")));

        assertThat(report.successRate()).isEqualTo(25);
        assertThat(report.recommendation()).contains("lacuna persistente");
        assertThat(report.identifiedGaps()).contains("Comparar fracoes");
    }
}
