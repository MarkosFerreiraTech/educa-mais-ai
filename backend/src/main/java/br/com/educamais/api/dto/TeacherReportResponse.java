package br.com.educamais.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeacherReportResponse(UUID id, UUID missionId, UUID studentId, String summary,
                                    List<String> identifiedGaps, String recommendation, int successRate,
                                    String deliveryStatus, Instant createdAt) {
}
