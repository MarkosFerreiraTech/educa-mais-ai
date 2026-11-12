package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.enums.StepStatus;
import br.com.educamais.domain.exception.InvalidMissionTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Missao de recuperacao -- agregado raiz do ciclo autonomo.
 * Protege as transicoes de estado (docs/arquitetura-agente.md 5.1).
 */
public class Mission {

    private final UUID id;
    private final UUID attendanceId;
    private final UUID studentId;
    private final UUID lessonId;
    private MissionStatus status;
    private final PlanSource planSource;
    private final List<MissionStep> steps;
    private final Instant createdAt;
    private Instant completedAt;

    public Mission(UUID id, UUID attendanceId, UUID studentId, UUID lessonId, MissionStatus status,
                   PlanSource planSource, List<MissionStep> steps, Instant createdAt, Instant completedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.status = status == null ? MissionStatus.PLANNED : status;
        this.planSource = planSource == null ? PlanSource.LLM : planSource;
        this.steps = new ArrayList<>(steps == null ? List.of() : steps);
        this.steps.sort(Comparator.comparingInt(MissionStep::order));
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.completedAt = completedAt;
    }

    public static Mission plan(UUID attendanceId, UUID studentId, UUID lessonId, MissionPlan plan) {
        List<MissionStep> plannedSteps = new ArrayList<>();
        int order = 1;
        for (MissionPlan.PlannedStep step : plan.steps()) {
            plannedSteps.add(MissionStep.planned(order++, step.objective(), step.explanation(), step.example()));
        }
        return new Mission(null, attendanceId, studentId, lessonId, MissionStatus.PLANNED,
                plan.source(), plannedSteps, Instant.now(), null);
    }

    public void markNotified() {
        transitionTo(MissionStatus.NOTIFIED);
    }

    public void start() {
        if (status == MissionStatus.NOTIFIED || status == MissionStatus.PLANNED) {
            transitionTo(MissionStatus.IN_PROGRESS);
        }
        currentStep().ifPresent(MissionStep::start);
    }

    /** RF19: conclui o passo atual e, se nao houver mais passos, finaliza a missao. */
    public void completeCurrentStep() {
        MissionStep step = currentStep().orElseThrow(
                () -> new InvalidMissionTransitionException("Missao sem passo em andamento"));
        step.complete();
        Optional<MissionStep> next = currentStep();
        next.ifPresent(MissionStep::start);
        if (next.isEmpty()) {
            MissionStatus finalStatus = steps.stream()
                    .anyMatch(s -> s.status() == StepStatus.COMPLETED_WITH_SUPPORT
                            || s.status() == StepStatus.NEEDS_REINFORCEMENT)
                    ? MissionStatus.COMPLETED_WITH_SUPPORT
                    : MissionStatus.COMPLETED;
            transitionTo(finalStatus);
            this.completedAt = Instant.now();
        }
    }

    /** RF18: aplica adaptacao pedagogica no passo atual. */
    public MissionStep adaptCurrentStep() {
        MissionStep step = currentStep().orElseThrow(
                () -> new InvalidMissionTransitionException("Missao sem passo em andamento"));
        step.registerFailure();
        return step;
    }

    public void applyExplanation(String explanation, String example) {
        currentStep().ifPresent(step -> step.applyNewExplanation(explanation, example));
    }

    public void cancel() {
        transitionTo(MissionStatus.CANCELLED);
    }

    public void abandon() {
        transitionTo(MissionStatus.ABANDONED);
    }

    /** RN02: concluida = todos os passos resolvidos (mesmo com reforco). */
    public boolean isFinished() {
        return status.isFinished();
    }

    public boolean isRewardGranted() {
        return isFinished();
    }

    public int progressPercent() {
        if (steps.isEmpty()) {
            return 0;
        }
        long done = steps.stream()
                .filter(step -> step.status() == StepStatus.COMPLETED
                        || step.status() == StepStatus.COMPLETED_WITH_SUPPORT
                        || step.status() == StepStatus.NEEDS_REINFORCEMENT)
                .count();
        return (int) (done * 100 / steps.size());
    }

    public int totalAttempts() {
        return steps.stream().mapToInt(MissionStep::attempts).sum();
    }

    public Optional<MissionStep> currentStep() {
        return steps.stream()
                .filter(step -> step.status() == StepStatus.PENDING
                        || step.status() == StepStatus.IN_PROGRESS
                        || step.status() == StepStatus.NEEDS_REINFORCEMENT)
                .findFirst();
    }

    private void transitionTo(MissionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidMissionTransitionException(
                    "Transicao invalida de %s para %s".formatted(status, target));
        }
        this.status = target;
    }

    public UUID id() {
        return id;
    }

    public UUID attendanceId() {
        return attendanceId;
    }

    public UUID studentId() {
        return studentId;
    }

    public UUID lessonId() {
        return lessonId;
    }

    public MissionStatus status() {
        return status;
    }

    public PlanSource planSource() {
        return planSource;
    }

    public List<MissionStep> steps() {
        return List.copyOf(steps);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}
