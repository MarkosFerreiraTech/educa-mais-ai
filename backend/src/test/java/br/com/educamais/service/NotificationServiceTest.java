package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Notification;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.NotificationPort;
import br.com.educamais.domain.port.NotificationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** RF23-RF26, RN09: notificacao multicanal com fallback. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationPort inAppAdapter;
    @Mock
    private NotificationPort whatsAppAdapter;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, inAppAdapter, whatsAppAdapter,
                new AttendanceMetrics(new SimpleMeterRegistry()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inAppAdapter.send(any())).thenReturn(NotificationPort.DeliveryResult.sent("inapp-1"));
        when(whatsAppAdapter.send(any())).thenReturn(NotificationPort.DeliveryResult.sent("SM123"));
    }

    private Student student(boolean optIn, String phone) {
        Student student = Student.register("Ana Souza", "ana@escola.br", phone, null, "hash", null, null, optIn);
        return student;
    }

    private Mission mission() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("A", "e", "x"),
                new MissionPlan.PlannedStep("B", "e", "y")), br.com.educamais.domain.enums.PlanSource.LLM);
        return Mission.plan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), plan);
    }

    @Test
    @DisplayName("Notificacao in-app e sempre enviada")
    void inApp() {
        Notification notification = service.notifyInApp(student(true, "+5581999998888"), mission(), "Fracoes");
        assertThat(notification.status().name()).isEqualTo("SENT");
        assertThat(notification.providerMessageId()).isEqualTo("inapp-1");
    }

    @Test
    @DisplayName("WhatsApp e enviado para aluno com opt-in e telefone")
    void whatsAppEnviado() {
        Notification notification = service.notifyWhatsApp(student(true, "+5581999998888"), mission(), "Fracoes");
        assertThat(notification.status().name()).isEqualTo("SENT");
        assertThat(notification.providerMessageId()).isEqualTo("SM123");
    }

    @Test
    @DisplayName("Sem opt-in o canal e SKIPPED com motivo (RN09)")
    void semOptIn() {
        Notification notification = service.notifyWhatsApp(student(false, "+5581999998888"), mission(), "Fracoes");
        assertThat(notification.status().name()).isEqualTo("SKIPPED");
        assertThat(notification.failureReason()).isEqualTo("NO_OPT_IN");
    }

    @Test
    @DisplayName("Sem telefone o canal e SKIPPED com motivo")
    void semTelefone() {
        Notification notification = service.notifyWhatsApp(student(true, null), mission(), "Fracoes");
        assertThat(notification.status().name()).isEqualTo("SKIPPED");
        assertThat(notification.failureReason()).isEqualTo("NO_PHONE");
    }

    @Test
    @DisplayName("Falha do provedor e registrada sem interromper o fluxo (RF25)")
    void falhaDoProvedor() {
        when(whatsAppAdapter.send(any())).thenReturn(NotificationPort.DeliveryResult.failed("HTTP 500"));

        Notification notification = service.notifyWhatsApp(student(true, "+5581999998888"), mission(), "Fracoes");

        assertThat(notification.status().name()).isEqualTo("FAILED");
        assertThat(notification.failureReason()).isEqualTo("HTTP 500");
    }
}
