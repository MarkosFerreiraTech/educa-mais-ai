package br.com.educamais.domain.port;

/**
 * Porta de notificacao proativa (Adapter: WhatsApp / In-app).
 */
public interface NotificationPort {

    DeliveryResult send(DeliveryRequest request);

    enum DeliveryStatus {
        SENT,
        FAILED,
        SKIPPED
    }

    record DeliveryRequest(String destination, String studentName, String topic, String missionLink) {
    }

    record DeliveryResult(DeliveryStatus status, String providerMessageId, String failureReason) {

        public static DeliveryResult sent(String providerMessageId) {
            return new DeliveryResult(DeliveryStatus.SENT, providerMessageId, null);
        }

        public static DeliveryResult failed(String reason) {
            return new DeliveryResult(DeliveryStatus.FAILED, null, reason);
        }

        public static DeliveryResult skipped(String reason) {
            return new DeliveryResult(DeliveryStatus.SKIPPED, null, reason);
        }
    }
}
