package br.com.educamais.domain.model;

import java.util.List;

/** Consolidado de desempenho usado no relatorio ao professor. */
public record AnswerSummary(int correct, int partial, int incorrect, List<String> gaps) {

    public AnswerSummary {
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
    }

    public static AnswerSummary empty() {
        return new AnswerSummary(0, 0, 0, List.of());
    }

    public static AnswerSummary of(List<DiagnosticAttempt> attempts, List<String> gaps) {
        int correct = (int) attempts.stream()
                .filter(attempt -> attempt.classification().name().equals("CORRECT"))
                .count();
        int partial = (int) attempts.stream()
                .filter(attempt -> attempt.classification().name().equals("PARTIAL"))
                .count();
        int incorrect = attempts.size() - correct - partial;
        return new AnswerSummary(correct, partial, incorrect, gaps);
    }

    public int total() {
        return correct + partial + incorrect;
    }

    public int successRate() {
        if (total() == 0) {
            return 100;
        }
        return (int) Math.round((correct * 100.0) / total());
    }
}
