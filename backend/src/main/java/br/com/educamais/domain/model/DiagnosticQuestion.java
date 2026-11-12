package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.QuestionType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Exercicio diagnostico (RF16). */
public record DiagnosticQuestion(UUID id, UUID missionId, UUID stepId, String prompt, QuestionType type,
                                 List<String> options, String correctAnswer, String explanation,
                                 int difficulty) {

    public DiagnosticQuestion {
        id = id == null ? UUID.randomUUID() : id;
        options = options == null ? List.of() : List.copyOf(options);
        type = type == null ? QuestionType.MULTIPLE_CHOICE : type;
        difficulty = Math.min(Math.max(difficulty, 1), 5);
    }

    /** Gabarito enxuto para o cliente: nunca expoe a resposta correta. */
    public boolean matches(String answer) {
        if (answer == null || correctAnswer == null) {
            return false;
        }
        return normalize(correctAnswer).equals(normalize(answer));
    }

    public DiagnosticQuestion withDifficulty(int newDifficulty) {
        return new DiagnosticQuestion(id, missionId, stepId, prompt, type, options, correctAnswer,
                explanation, newDifficulty);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
