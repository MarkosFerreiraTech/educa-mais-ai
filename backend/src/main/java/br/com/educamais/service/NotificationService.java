package br.com.educamais.service;

import br.com.educamais.domain.enums.NotificationChannel;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.Notification;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.NotificationPort;
import br.com.educamais.domain.port.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Orquestra as notificacoes proativas (RF23-RF26).
 * O canal WhatsApp nunca interrompe o fluxo: falhas viram status e metricas.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationPort inAppPort;
    private final NotificationPort whatsAppPort;
    private final AttendanceMetrics metrics;

    public NotificationService(NotificationRepository repository,
                               @Qualifier("inAppNotificationAdapter") NotificationPort inAppPort,
                               @Qualifier("whatsAppNotificationAdapter") NotificationPort whatsAppPort,
                               AttendanceMetrics metrics) {
        this.repository = repository;
        this.inAppPort = inAppPort;
        this.whatsAppPort = whatsAppPort;
        this.metrics = metrics;
    }

    /** RF23: notificacao in-app sempre criada. */
    public Notification notifyInApp(Student student, Mission mission, String topic) {
        Notification notification = Notification.pending(student.id(), mission.id(),
                NotificationChannel.IN_APP, "Missao de recuperacao disponivel: " + topic);
        NotificationPort.DeliveryResult result = inAppPort.send(
                new NotificationPort.DeliveryRequest(null, student.firstName(), topic, missionLink(mission)));
        notification.markSent(result.providerMessageId());
        return repository.save(notification);
    }

    /** RF24/RF25/RN09: disparo WhatsApp com fallback e registro de status. */
    public Notification notifyWhatsApp(Student student, Mission mission, String topic) {
        Notification notification = Notification.pending(student.id(), mission.id(),
                NotificationChannel.WHATSAPP, "Lembrete de recuperacao: " + topic);
        if (!student.notificationOptIn()) {
            notification.markSkipped("NO_OPT_IN");
            metrics.whatsappSkipped();
            return repository.save(notification);
        }
        if (student.phone() == null || student.phone().isBlank()) {
            notification.markSkipped("NO_PHONE");
            metrics.whatsappSkipped();
            return repository.save(notification);
        }
        NotificationPort.DeliveryResult result = whatsAppPort.send(new NotificationPort.DeliveryRequest(
                student.phone(), student.firstName(), topic, missionLink(mission)));
        applyResult(notification, result);
        return repository.save(notification);
    }

    private void applyResult(Notification notification, NotificationPort.DeliveryResult result) {
        switch (result.status()) {
            case SENT -> notification.markSent(result.providerMessageId());
            case FAILED -> {
                notification.markFailed(result.failureReason());
                metrics.whatsappFailure();
                log.warn("NOTIFICATION_WHATSAPP_FAILED reason={}", result.failureReason());
            }
            case SKIPPED -> {
                notification.markSkipped(result.failureReason());
                metrics.whatsappSkipped();
            }
        }
    }

    public List<Notification> forStudent(UUID studentId) {
        return repository.findByStudentId(studentId);
    }

    public List<Notification> forMission(UUID missionId) {
        return repository.findByMissionId(missionId);
    }

    private String missionLink(Mission mission) {
        return "/aluno/missoes/" + mission.id();
    }
}
