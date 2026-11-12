package br.com.educamais.infrastructure.notification;

import br.com.educamais.domain.port.NotificationPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adapter de notificacao in-app (RF23): canal garantido, sempre disponivel. */
@Component("inAppNotificationAdapter")
public class InAppNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationAdapter.class);

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String messageId = "inapp-" + UUID.randomUUID();
        log.info("NOTIFICATION_IN_APP_SENT studentFirstName={} messageRef={}", request.studentName(), messageId);
        return DeliveryResult.sent(messageId);
    }
}
