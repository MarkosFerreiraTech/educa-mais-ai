package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.enums.StepStatus;
import java.util.UUID;

/** Passo da trilha de recuperacao com estrategia e contador de tentativas (RN05). */
public class MissionStep {

    public static final int MAX_ATTEMPTS = 3;

    private final UUID id;
    private final int order;
    private final String objective;
    private String explanation;
    private String example;
    private PedagogicalStrategyType strategy;
    private StepStatus status;
    private int attempts;

    public MissionStep(UUID id, int order, String objective, String explanation, String example,
                       PedagogicalStrategyType strategy, StepStatus status, int attempts) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.order = order;
        this.objective = objective;
        this.explanation = explanation;
        this.example = example;
        this.strategy = strategy == null ? PedagogicalStrategyType.EXPLICIT : strategy;
        this.status = status == null ? StepStatus.PENDING : status;
        this.attempts = Math.max(attempts, 0);
    }

    public static MissionStep planned(int order, String objective, String explanation, String example) {
        return new MissionStep(null, order, objective, explanation, example,
                PedagogicalStrategyType.EXPLICIT, StepStatus.PENDING, 0);
    }

    public void start() {
        if (status == StepStatus.PENDING) {
            status = StepStatus.IN_PROGRESS;
        }
    }

    /** RF19: acerto conclui o passo. */
    public void complete() {
        this.status = attempts > 0 ? StepStatus.COMPLETED_WITH_SUPPORT : StepStatus.COMPLETED;
    }

    /** RF18/RN05: registra o erro e escala a estrategia pedagogica. */
    public PedagogicalStrategyType registerFailure() {
        this.attempts++;
        this.status = StepStatus.IN_PROGRESS;
        if (attempts >= MAX_ATTEMPTS) {
            this.strategy = PedagogicalStrategyType.STEP_BY_STEP;
            this.status = StepStatus.NEEDS_REINFORCEMENT;
        } else {
            this.strategy = strategy.escalate();
        }
        return this.strategy;
    }

    public boolean canRetry() {
        return attempts < MAX_ATTEMPTS;
    }

    public void applyNewExplanation(String newExplanation, String newExample) {
        this.explanation = newExplanation;
        this.example = newExample;
    }

    public UUID id() {
        return id;
    }

    public int order() {
        return order;
    }

    public String objective() {
        return objective;
    }

    public String explanation() {
        return explanation;
    }

    public String example() {
        return example;
    }

    public PedagogicalStrategyType strategy() {
        return strategy;
    }

    public StepStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }
}
