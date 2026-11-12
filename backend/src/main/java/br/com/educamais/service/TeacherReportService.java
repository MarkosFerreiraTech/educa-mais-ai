package br.com.educamais.service;

import br.com.educamais.agent.planner.PromptFactory;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.AnswerSummary;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionStep;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.TeacherReport;
import br.com.educamais.domain.port.LlmPort;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.ReportRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatorio automatico ao professor (RF22, RF33 / UC07).
 * Se o LLM falhar, aplica recomendacao baseada em regras -- o relatorio nunca deixa de ser entregue.
 */
@Service
public class TeacherReportService {

    private final ReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final LlmPort llmPort;
    private final PromptFactory promptFactory;
    private final AttendanceMetrics metrics;

    public TeacherReportService(ReportRepository reportRepository, StudentRepository studentRepository,
                                LessonRepository lessonRepository, LlmPort llmPort,
                                PromptFactory promptFactory, AttendanceMetrics metrics) {
        this.reportRepository = reportRepository;
        this.studentRepository = studentRepository;
        this.lessonRepository = lessonRepository;
        this.llmPort = llmPort;
        this.promptFactory = promptFactory;
        this.metrics = metrics;
    }

    @Transactional
    public TeacherReport generate(Mission mission, AnswerSummary summary) {
        Optional<TeacherReport> existing = reportRepository.findByMissionId(mission.id());
        if (existing.isPresent()) {
            return existing.get();
        }
        Student student = studentRepository.findById(mission.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", mission.studentId()));
        List<String> gaps = summary.gaps().isEmpty() ? inferGaps(mission) : summary.gaps();
        int successRate = summary.successRate();
        var topic = lessonRepository.findById(mission.lessonId())
                .map(lesson -> lesson.topic())
                .orElse("conteudo da aula");

        String recommendation = llmPort.recommendTeacherAction(new LlmPort.RecommendationPrompt(
                        promptFactory.recommendationPrompt(student.firstName(), topic, successRate,
                                mission.totalAttempts(), gaps)))
                .orElseGet(() -> ruleBasedRecommendation(successRate, gaps));

        String summaryText = "Recuperacao de %s em %s: %d%% de acerto, %d tentativas."
                .formatted(student.firstName(), topic, successRate, mission.totalAttempts());

        TeacherReport report = new TeacherReport(null, mission.id(), mission.studentId(), null, summaryText,
                gaps, recommendation, successRate, "DELIVERED", null);
        TeacherReport saved = reportRepository.save(report);
        metrics.reportDelivered();
        return saved;
    }

    public Optional<TeacherReport> byMission(UUID missionId) {
        return reportRepository.findByMissionId(missionId);
    }

    private List<String> inferGaps(Mission mission) {
        return mission.steps().stream()
                .filter(step -> step.attempts() > 0)
                .map(MissionStep::objective)
                .toList();
    }

    private static String ruleBasedRecommendation(int successRate, List<String> gaps) {
        if (successRate >= 85) {
            return "Aluno recuperou o conteudo com autonomia. Sugerimos apenas acompanhamento leve "
                    + "e um exercicio de ampliacao na proxima aula.";
        }
        if (successRate >= 60) {
            return "Aluno recuperou o conteudo com apoio em '%s'. Recomendamos retomada breve em sala "
                    .formatted(String.join(", ", gaps)) + "com um exemplo guiado.";
        }
        return "Aluno apresentou lacuna persistente em '%s'. Recomendamos intervencao direta do professor "
                .formatted(String.join(", ", gaps)) + "com retomada em pequenos passos e verificacao diaria.";
    }
}
