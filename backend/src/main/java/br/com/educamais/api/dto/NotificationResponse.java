package br.com.educamais.api.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String channel, String status, String payload, String failureReason,
                                   Instant sentAt) {
}
