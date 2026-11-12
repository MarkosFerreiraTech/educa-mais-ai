package br.com.educamais.domain.event;

import java.util.UUID;

/** Evento disparado quando a missao atinge um estado final. */
public record MissionCompletedEvent(UUID missionId, UUID studentId, UUID lessonId, boolean withSupport) {
}
