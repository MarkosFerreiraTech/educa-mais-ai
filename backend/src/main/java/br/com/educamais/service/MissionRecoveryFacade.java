package br.com.educamais.service;

import br.com.educamais.agent.MissionOrchestrator;
import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.Student;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fachada da jornada do aluno: iniciar missao, obter exercicio, responder e consultar progresso
 * (RF27, RF16-RF19 / UC05).
 */
@Service
public class MissionRecoveryFacade {

    private final MissionService missionService;
    private final StudentService studentService;
    private final AssessmentService assessmentService;
    private final MissionOrchestrator orchestrator;

    public MissionRecoveryFacade(MissionService missionService, StudentService studentService,
                                 AssessmentService assessmentService, MissionOrchestrator orchestrator) {
        this.missionService = missionService;
        this.studentService = studentService;
        this.assessmentService = assessmentService;
        this.orchestrator = orchestrator;
    }

    public MissionJourney start(UUID missionId, UUID studentId) {
        Mission mission = missionService.start(missionId, studentId);
        return journey(mission, null);
    }

    public MissionJourney current(UUID missionId, UUID studentId) {
        Mission mission = missionService.getForStudent(missionId, studentId);
        DiagnosticQuestion question = mission.isFinished() ? null : assessmentService.prepareQuestion(mission);
        return journey(mission, question);
    }

    public MissionJourney answer(UUID missionId, UUID studentId, UUID questionId, String answer) {
        MissionOrchestrator.AnswerOutcome outcome = orchestrator.answer(missionId, studentId, questionId, answer);
        Mission mission = outcome.mission();
        DiagnosticQuestion question = mission.isFinished() ? null : assessmentService.prepareQuestion(mission);
        return new MissionJourney(mission, question, studentService.get(mission.studentId()),
                outcome.feedback(), outcome.classification());
    }

    public List<Mission> studentMissions(UUID studentId) {
        return missionService.listForStudent(studentId);
    }

    private MissionJourney journey(Mission mission, DiagnosticQuestion question) {
        Student student = studentService.get(mission.studentId());
        return new MissionJourney(mission, question, student, null, null);
    }

    /** Payload da jornada de recuperacao (missao + exercicio atual + ultimo feedback). */
    public record MissionJourney(Mission mission, DiagnosticQuestion question, Student student, String feedback,
                                 String classification) {

        public int progress() {
            return mission.progressPercent();
        }

        public boolean finished() {
            return mission.status() == MissionStatus.COMPLETED
                    || mission.status() == MissionStatus.COMPLETED_WITH_SUPPORT;
        }
    }
}
