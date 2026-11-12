package br.com.educamais.api.controller;

import br.com.educamais.api.dto.AttendanceWebhookRequest;
import br.com.educamais.api.dto.AttendanceWebhookResponse;
import br.com.educamais.infrastructure.messaging.AttendanceIngestionService;
import br.com.educamais.security.HmacSignatureValidator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook do sistema escolar (RF06-RF10 / UC03).
 * Unica rota publica, protegida por HMAC + API Key dedicada.
 */
@RestController
@RequestMapping("/api/v1/integrations/school")
public class SchoolIntegrationController {

    private final HmacSignatureValidator signatureValidator;
    private final AttendanceIngestionService ingestionService;

    public SchoolIntegrationController(HmacSignatureValidator signatureValidator,
                                       AttendanceIngestionService ingestionService) {
        this.signatureValidator = signatureValidator;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/attendance")
    public ResponseEntity<AttendanceWebhookResponse> receive(
            @RequestHeader(value = HmacSignatureValidator.HEADER_API_KEY, required = false) String apiKey,
            @RequestHeader(value = HmacSignatureValidator.HEADER_SIGNATURE, required = false) String signature,
            @RequestHeader(value = HmacSignatureValidator.HEADER_TIMESTAMP, required = false) String timestamp,
            @Valid @RequestBody AttendanceWebhookRequest request) {

        signatureValidator.validate(apiKey, signature, timestamp, toCanonicalPayload(request));
        AttendanceIngestionService.IngestionResult result = ingestionService.ingest(
                new AttendanceIngestionService.SchoolAttendancePayload(request.eventId(), request.studentId(),
                        request.lessonId(), request.occurredAt(), request.reason()));

        HttpStatus status = "IGNORED_DUPLICATE".equals(result.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(new AttendanceWebhookResponse(result.eventId(), result.status(),
                result.attendanceId() == null ? null : result.attendanceId().toString()));
    }

    /** Assinatura calculada sobre o payload canonico (eventId|studentId|lessonId). */
    static String toCanonicalPayload(AttendanceWebhookRequest request) {
        return request.eventId() + "|" + request.studentId() + "|" + request.lessonId();
    }
}
