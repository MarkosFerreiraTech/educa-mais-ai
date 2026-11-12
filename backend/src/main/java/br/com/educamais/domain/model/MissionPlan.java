package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.PlanSource;
import java.util.List;

/** Plano pedagogico gerado pelo agente (RF12/RF13). */
public record MissionPlan(List<PlannedStep> steps, PlanSource source) {

    public static final int MIN_STEPS = 2;
    public static final int MAX_STEPS = 5;

    public MissionPlan {
        steps = steps == null ? List.of() : List.copyOf(steps);
        source = source == null ? PlanSource.LLM : source;
    }

    public boolean isValid() {
        return steps.size() >= MIN_STEPS && steps.size() <= MAX_STEPS;
    }

    public record PlannedStep(String objective, String explanation, String example) {
        public PlannedStep {
            if (objective == null || objective.isBlank()) {
                throw new IllegalArgumentException("Objetivo do passo e obrigatorio");
            }
        }
    }
}
