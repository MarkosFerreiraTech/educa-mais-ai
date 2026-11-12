package br.com.educamais.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Relatorio automatico entregue ao professor (RF33). */
public record TeacherReport(UUID id, UUID missionId, UUID studentId, UUID teacherId, String summary,
                            List<String> identifiedGaps, String recommendation, int successRate,
                            String deliveryStatus, Instant createdAt) {

    public TeacherReport {
        id = id == null ? UUID.randomUUID() : id;
        identifiedGaps = identifiedGaps == null ? List.of() : List.copyOf(identifiedGaps);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        deliveryStatus = deliveryStatus == null ? "DELIVERED" : deliveryStatus;
    }
}
