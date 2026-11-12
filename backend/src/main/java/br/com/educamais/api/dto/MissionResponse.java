package br.com.educamais.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Representacao da missao para o aluno (RF27). */
public record MissionResponse(UUID id, String status, String planSource, int progress, int attempts,
                              boolean finished, Instant createdAt, Instant completedAt,
                              List<StepResponse> steps, QuestionResponse question) {

    /** Passo da trilha exposto ao cliente. */
    public record StepResponse(int order, String objective, String explanation, String example, String strategy,
                               String status, int attempts) {
    }

    /** Exercicio diagnostico atual. */
    public record QuestionResponse(UUID id, String prompt, String type, List<String> options, int difficulty) {
    }
}
