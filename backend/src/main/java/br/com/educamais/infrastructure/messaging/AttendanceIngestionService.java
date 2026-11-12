package br.com.educamais.infrastructure.messaging;

import br.com.educamais.domain.model.Attendance;
import br.com.educamais.service.AttendanceService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Caso de uso da ingestao assincrona (RF06-RF10): converte o payload do webhook em falta registrada
 * e delega a publicacao do evento ao {@link AttendanceService}.
 */
@Service
public class AttendanceIngestionService {

    private final AttendanceService attendanceService;

    public AttendanceIngestionService(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    public IngestionResult ingest(SchoolAttendancePayload payload) {
        String eventId = payload.eventId();
        if (eventId == null || eventId.isBlank()) {
            throw new br.com.educamais.domain.exception.BusinessRuleException("MISSING_EVENT_ID",
                    "eventId obrigatorio para idempotencia");
        }
        UUID studentId = UUID.fromString(payload.studentId());
        UUID lessonId = UUID.fromString(payload.lessonId());
        Instant occurredAt = payload.occurredAt() == null ? Instant.now() : payload.occurredAt();
        Optional<Attendance> attendance = attendanceService.registerAbsence(studentId, lessonId, eventId,
                occurredAt);
        return attendance.map(value -> new IngestionResult(value.id(), eventId, "ACCEPTED"))
                .orElseGet(() -> new IngestionResult(null, eventId, "IGNORED_DUPLICATE"));
    }

    /** Payload do sistema escolar (contrato do webhook). */
    public record SchoolAttendancePayload(String eventId, String studentId, String lessonId, Instant occurredAt,
                                         String reason) {
    }

    /** Resultado da ingestao retornado ao sistema escolar. */
    public record IngestionResult(UUID attendanceId, String eventId, String status) {
    }
}
