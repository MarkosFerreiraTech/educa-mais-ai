package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.NotificationChannel;
import br.com.educamais.domain.enums.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/** Notificacao proativa por canal (RF23-RF26). */
public class Notification {

    private final UUID id;
    private final UUID studentId;
    private final UUID missionId;
    private final NotificationChannel channel;
    private NotificationStatus status;
    private String providerMessageId;
    private String failureReason;
    private final String payload;
    private Instant sentAt;

    public Notification(UUID id, UUID studentId, UUID missionId, NotificationChannel channel,
                        NotificationStatus status, String payload) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.studentId = studentId;
        this.missionId = missionId;
        this.channel = channel;
        this.status = status == null ? NotificationStatus.PENDING : status;
        this.payload = payload;
    }

    public static Notification pending(UUID studentId, UUID missionId, NotificationChannel channel,
                                       String payload) {
        return new Notification(null, studentId, missionId, channel, NotificationStatus.PENDING, payload);
    }

    public void markSent(String providerMessageId) {
        this.status = NotificationStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.sentAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    public void markSkipped(String reason) {
        this.status = NotificationStatus.SKIPPED;
        this.failureReason = reason;
    }

    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }

    public UUID id() {
        return id;
    }

    public UUID studentId() {
        return studentId;
    }

    public UUID missionId() {
        return missionId;
    }

    public NotificationChannel channel() {
        return channel;
    }

    public NotificationStatus status() {
        return status;
    }

    public String providerMessageId() {
        return providerMessageId;
    }

    public String failureReason() {
        return failureReason;
    }

    public String payload() {
        return payload;
    }

    public Instant sentAt() {
        return sentAt;
    }
}
