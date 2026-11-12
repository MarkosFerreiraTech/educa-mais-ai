package br.com.educamais.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** Contrato do webhook escolar (RF06). */
public record AttendanceWebhookRequest(
        @NotBlank(message = "eventId e obrigatorio") String eventId,
        @NotBlank(message = "studentId e obrigatorio") String studentId,
        @NotBlank(message = "lessonId e obrigatorio") String lessonId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt,
        String reason) {
}
