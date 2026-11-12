package br.com.educamais.domain.enums;

import java.util.Set;

/**
 * Estados da missao de recuperacao e transicoes validas (State pattern simplificado).
 * Referencia: docs/arquitetura-agente.md secao 5.1
 */
public enum MissionStatus {
    PLANNED,
    NOTIFIED,
    IN_PROGRESS,
    COMPLETED,
    COMPLETED_WITH_SUPPORT,
    ABANDONED,
    CANCELLED,
    BLOCKED_NO_SYLLABUS;

    public boolean canTransitionTo(MissionStatus target) {
        return switch (this) {
            case PLANNED -> Set.of(NOTIFIED, BLOCKED_NO_SYLLABUS, CANCELLED).contains(target);
            case NOTIFIED -> Set.of(IN_PROGRESS, CANCELLED, ABANDONED).contains(target);
            case IN_PROGRESS -> Set.of(COMPLETED, COMPLETED_WITH_SUPPORT, ABANDONED).contains(target);
            case BLOCKED_NO_SYLLABUS -> target == PLANNED;
            default -> false;
        };
    }

    public boolean isFinished() {
        return this == COMPLETED || this == COMPLETED_WITH_SUPPORT;
    }
}
