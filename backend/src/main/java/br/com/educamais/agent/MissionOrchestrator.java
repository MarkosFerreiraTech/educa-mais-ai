package br.com.educamais.agent;

import br.com.educamais.agent.audit.AgentAuditTrail;
import br.com.educamais.agent.planner.RecoveryPlanRequest;
import br.com.educamais.agent.planner.RecoveryPlanner;
import br.com.educamais.domain.enums.AgentAction;
import br.com.educamais.domain.enums.AnswerClassification;
import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.AnswerSummary;
import br.com.educamais.domain.model.DiagnosticAttempt;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.MissionStep;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.AttendanceRepository;
import br.com.educamais.domain.port.DiagnosticRepository;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.MissionRepository;
import br.com.educamais.domain.port.StudentRepository;
import br.com.educamais.service.AttendanceMetrics;
import br.com.educamais.service.AssessmentService;
import br.com.educamais.service.NotificationService;
import br.com.educamais.service.RewardService;
import br.com.educamais.service.TeacherReportService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestrador do ciclo autonomo do agente:
 * OBSERVAR -> PLANEJAR -> AGIR -> ADAPTAR -> RECOMPENSAR -> REPORTAR
 * (RF12-RF26, RF20; docs/arquitetura-agente.md secao 5).
 */
@Service
public class MissionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MissionOrchestrator.class);

    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final AttendanceRepository attendanceRepository;
    private final MissionRepository missionRepository;
    private final DiagnosticRepository diagnosticRepository;
    private final RecoveryPlanner planner;
    private final NotificationService notificationService;
    private final AssessmentService assessmentService;
    private final RewardService rewardService;
    private final TeacherReportService reportService;
    private final AgentAuditTrail auditTrail;
    private final AttendanceMetrics metrics;

    public MissionOrchestrator(StudentRepository studentRepository, LessonRepository lessonRepository,
                               AttendanceRepository attendanceRepository, MissionRepository missionRepository,
                               DiagnosticRepository diagnosticRepository, RecoveryPlanner planner,
                               NotificationService notificationService, AssessmentService assessmentService,
                               RewardService rewardService, TeacherReportService reportService,
                               AgentAuditTrail auditTrail, AttendanceMetrics metrics) {
        this.studentRepository = studentRepository;
        this.lessonRepository = lessonRepository;
        this.attendanceRepository = attendanceRepository;
        this.missionRepository = missionRepository;
        this.diagnosticRepository = diagnosticRepository;
        this.planner = planner;
        this.notificationService = notificationService;
        this.assessmentService = assessmentService;
        this.rewardService = rewardService;
        this.reportService = reportService;
        this.auditTrail = auditTrail;
        this.metrics = metrics;
    }

    /** Executa o ciclo a partir de uma falta registrada. Idempotente por falta (RN06). */
    @Transactional
    public Mission runForAttendance(UUID attendanceId) {
        long startedAt = System.currentTimeMillis();
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Falta", attendanceId));

        Optional<Mission> existing = missionRepository.findByAttendanceId(attendanceId);
        if (existing.isPresent()) {
            log.info("AGENT_MISSION_REUSED missionRef={}", existing.get().id());
            return existing.get();
        }

        Student student = studentRepository.findById(attendance.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", attendance.studentId()));
        Lesson lesson = lessonRepository.findById(attendance.lessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Aula", attendance.lessonId()));

        auditTrail.record(null, AgentAction.OBSERVE, "Falta observada: " + lesson.topic(),
                Map.of("attendanceRef", attendanceId.toString(), "lessonRef", lesson.id().toString()),
                System.currentTimeMillis() - startedAt);

        if (!lesson.hasSyllabus()) {
            Mission blocked = missionRepository.save(new Mission(null, attendanceId, student.id(), lesson.id(),
                    MissionStatus.BLOCKED_NO_SYLLABUS, br.com.educamais.domain.enums.PlanSource.FALLBACK,
                    List.of(), null, null));
            log.warn("AGENT_MISSION_BLOCKED_NO_SYLLABUS lessonRef={}", lesson.id());
            return blocked;
        }

        RecoveryPlanRequest planRequest = new RecoveryPlanRequest(student, lesson, student.primaryInterest()
                .orElse(null), attendance.eventId());
        RecoveryPlanner.PlanningResult planning = planner.plan(planRequest);
        if (!planning.plan().isValid()) {
            // Plano parcialmente descartado pelo grounding: completa com o fallback ancorado na ementa.
            planning = new RecoveryPlanner.PlanningResult(
                    planner.fallback(planRequest), planning.discardedObjectives(), planning.latencyMs(),
                    br.com.educamais.domain.enums.PlanSource.FALLBACK);
        }
        if (planning.source() == br.com.educamais.domain.enums.PlanSource.LLM) {
            metrics.planFromLlm();
        } else {
            metrics.planFromFallback();
        }

        Mission mission = new Mission(null, attendanceId, student.id(), lesson.id(), MissionStatus.PLANNED,
                planning.source(), toSteps(planning.plan()), null, null);
        Mission saved = missionRepository.save(mission);
        auditTrail.record(saved.id(), AgentAction.PLAN, "Plano gerado (" + planning.source() + ")",
                Map.of("steps", planning.plan().steps().size(), "discarded", planning.discardedObjectives()),
                planning.latencyMs());

        notificationService.notifyInApp(student, saved, lesson.topic());
        notificationService.notifyWhatsApp(student, saved, lesson.topic());
        metrics.recordAttendanceToNotification(System.currentTimeMillis() - startedAt);
        auditTrail.record(saved.id(), AgentAction.ACT, "Notificacoes disparadas (in-app + WhatsApp)",
                Map.of("channels", List.of("IN_APP", "WHATSAPP")), System.currentTimeMillis() - startedAt);

        saved.markNotified();
        return missionRepository.save(saved);
    }

    /** RF17-RF19: processa a resposta do aluno e decide avancar, adaptar ou concluir. */
    @Transactional
    public AnswerOutcome answer(UUID missionId, UUID studentId, UUID questionId, String answer) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Missao", missionId));
        if (!mission.studentId().equals(studentId)) {
            throw new br.com.educamais.domain.exception.AccessDeniedException("Missao de outro aluno");
        }
        Student student = studentRepository.findById(studentId).orElseThrow();
        mission.start();

        AssessmentService.Evaluation evaluation = assessmentService.evaluate(questionId, studentId, answer,
                student.primaryInterest().map(code -> code.contextHint()).orElse("cotidiano"));

        if (evaluation.classification() == AnswerClassification.CORRECT) {
            mission.completeCurrentStep();
            auditTrail.record(mission.id(), AgentAction.ADAPT, "Acerto: passo concluido",
                    Map.of("classification", "CORRECT"), 0);
        } else {
            AssessmentService.Adaptation adaptation = assessmentService.adapt(mission, student, answer);
            auditTrail.record(mission.id(), AgentAction.ADAPT,
                    "Erro: estrategia ajustada para " + adaptation.strategy(),
                    Map.of("strategy", adaptation.strategy().name(),
                            "reinforcement", adaptation.reinforcement(),
                            "llm", adaptation.generatedByLlm()), 0);
        }

        Mission persisted = missionRepository.save(mission);
        if (persisted.isFinished()) {
            finishCycle(persisted, student);
        }
        Mission reloaded = missionRepository.findById(missionId).orElse(persisted);
        return new AnswerOutcome(reloaded, evaluation.classification().name(), evaluation.feedback());
    }

    /** Resultado do processamento de uma resposta do aluno. */
    public record AnswerOutcome(Mission mission, String classification, String feedback) {
    }

    /** Recupera o eventId da falta (chave estavel de idempotencia da recompensa - RN03). */
    private String resolveEventId(Mission mission) {
        return attendanceRepository.findById(mission.attendanceId()).map(a -> a.eventId()).orElse(null);
    }

    private static List<br.com.educamais.domain.model.MissionStep> toSteps(MissionPlan plan) {
        List<br.com.educamais.domain.model.MissionStep> steps = new java.util.ArrayList<>();
        int order = 1;
        for (MissionPlan.PlannedStep planned : plan.steps()) {
            steps.add(br.com.educamais.domain.model.MissionStep.planned(order++, planned.objective(),
                    planned.explanation(), planned.example()));
        }
        return steps;
    }

    /** RECOMPENSAR + REPORTAR (RF22, RF29, RF33). */
    private void finishCycle(Mission mission, Student student) {
        RewardService.RewardGrant grant = rewardService.grantMissionReward(mission, resolveEventId(mission));
        auditTrail.record(mission.id(), AgentAction.REWARD,
                "Recompensa concedida: +" + grant.xpGranted() + " XP, +" + grant.diamondsGranted() + " diamante",
                Map.of("newlyGranted", grant.newlyGranted()), 0);

        List<DiagnosticAttempt> attempts = diagnosticRepository.findAttemptsByStudentId(student.id());
        List<MissionStep> steps = mission.steps();
        AnswerSummary summary = AnswerSummary.of(attempts, steps.stream()
                .filter(step -> step.attempts() > 0)
                .map(MissionStep::objective)
                .toList());
        var report = reportService.generate(mission, summary);
        auditTrail.record(mission.id(), AgentAction.REPORT,
                "Relatorio entregue ao professor", Map.of("reportRef", report.id().toString()), 0);
        log.info("AGENT_CYCLE_FINISHED missionRef={} status={}", mission.id(), mission.status());
    }
}
