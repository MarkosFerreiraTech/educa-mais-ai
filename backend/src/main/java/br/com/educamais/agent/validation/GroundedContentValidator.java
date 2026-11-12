package br.com.educamais.agent.validation;

import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Syllabus;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Garante que o plano do LLM permaneca ancorado na ementa (RF15 / R4 do charter).
 * Passos cujo objetivo e explicacao nao compartilham nenhum termo com a ementa sao descartados.
 */
@Component
public class GroundedContentValidator {

    private static final Logger log = LoggerFactory.getLogger(GroundedContentValidator.class);
    private static final int MIN_KEYWORD_LENGTH = 4;
    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "dos", "das", "que", "uma", "por", "como", "sobre", "entre", "aula",
            "aluno", "aluna", "voce", "esse", "essa", "isso", "mais", "menos", "pelo", "pela");

    public ValidationOutcome validate(MissionPlan plan, Syllabus syllabus) {
        if (plan == null || plan.steps().isEmpty()) {
            return new ValidationOutcome(null, List.of("plano vazio"));
        }
        Set<String> allowed = keywords(String.join(" ", syllabus.learningObjectives())
                + " " + String.join(" ", syllabus.topics()));
        List<String> discarded = new java.util.ArrayList<>();
        List<MissionPlan.PlannedStep> approved = new java.util.ArrayList<>();
        for (MissionPlan.PlannedStep step : plan.steps()) {
            Set<String> stepKeywords = keywords(step.objective() + " " + nullSafe(step.explanation()));
            boolean grounded = allowed.isEmpty() || stepKeywords.stream().anyMatch(allowed::contains);
            if (grounded) {
                approved.add(step);
            } else {
                discarded.add(step.objective());
            }
        }
        if (!discarded.isEmpty()) {
            log.info("AGENT_GROUNDING_DISCARDED steps={} reasons={}", discarded.size(), discarded);
        }
        if (approved.isEmpty() && !plan.steps().isEmpty()) {
            // Grounding total: preserva o primeiro passo (objetivo veio do LLM, alinhado ao prompt).
            approved.add(plan.steps().get(0));
            discarded.remove(plan.steps().get(0).objective());
        }
        MissionPlan sanitized = new MissionPlan(approved, plan.source());
        return new ValidationOutcome(sanitized, discarded);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    static Set<String> keywords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(word -> word.length() >= MIN_KEYWORD_LENGTH)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(java.util.stream.Collectors.toSet());
    }

    /** Resultado da validacao: plano saneado + objetivos descartados (audit trail). */
    public record ValidationOutcome(MissionPlan plan, List<String> discardedObjectives) {

        public boolean hasValidPlan() {
            return plan != null && plan.isValid();
        }
    }
}
