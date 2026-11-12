package br.com.educamais.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Evento de dominio publicado apos a falta ser persistida (Observer). */
public record AttendanceRegisteredEvent(UUID attendanceId, UUID studentId, UUID lessonId,
                                        String eventId, Instant occurredAt) {
}
