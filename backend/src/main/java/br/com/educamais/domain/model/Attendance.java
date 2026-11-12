package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.AttendanceOrigin;
import br.com.educamais.domain.enums.AttendanceStatus;
import java.time.Instant;
import java.util.UUID;

/** Falta registrada. {@code eventId} garante idempotencia (RF09). */
public record Attendance(UUID id, UUID studentId, UUID lessonId, String eventId,
                         AttendanceStatus status, AttendanceOrigin origin, Instant occurredAt) {

    public Attendance {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        origin = origin == null ? AttendanceOrigin.WEBHOOK : origin;
        status = status == null ? AttendanceStatus.ABSENT : status;
    }

    public static Attendance fromWebhook(UUID studentId, UUID lessonId, String eventId, Instant occurredAt) {
        return new Attendance(UUID.randomUUID(), studentId, lessonId, eventId,
                AttendanceStatus.ABSENT, AttendanceOrigin.WEBHOOK, occurredAt);
    }

    public Attendance justify() {
        return new Attendance(id, studentId, lessonId, eventId, AttendanceStatus.JUSTIFIED, origin, occurredAt);
    }

    public boolean isAbsent() {
        return status == AttendanceStatus.ABSENT;
    }

    public boolean isJustified() {
        return status == AttendanceStatus.JUSTIFIED;
    }
}
