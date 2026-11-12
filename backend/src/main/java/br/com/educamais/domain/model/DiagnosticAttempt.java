package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.AnswerClassification;
import java.time.Instant;
import java.util.UUID;

/** Tentativa de resposta do aluno, com feedback do agente (RF17). */
public record DiagnosticAttempt(UUID id, UUID questionId, UUID studentId, String answer,
                                AnswerClassification classification, String feedback,
                                Instant answeredAt, int timeSpentSeconds) {

    public DiagnosticAttempt {
        id = id == null ? UUID.randomUUID() : id;
        answeredAt = answeredAt == null ? Instant.now() : answeredAt;
    }

    public static DiagnosticAttempt of(UUID questionId, UUID studentId, String answer,
                                       AnswerClassification classification, String feedback) {
        return new DiagnosticAttempt(null, questionId, studentId, answer, classification, feedback,
                Instant.now(), 0);
    }
}
