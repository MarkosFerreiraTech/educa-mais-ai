package br.com.educamais.domain.port;

import br.com.educamais.domain.model.DiagnosticAttempt;
import br.com.educamais.domain.model.DiagnosticQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticRepository {

    DiagnosticQuestion saveQuestion(DiagnosticQuestion question);

    Optional<DiagnosticQuestion> findQuestionById(UUID id);

    List<DiagnosticQuestion> findQuestionsByMissionId(UUID missionId);

    DiagnosticAttempt saveAttempt(DiagnosticAttempt attempt);

    List<DiagnosticAttempt> findAttemptsByStudentId(UUID studentId);
}
