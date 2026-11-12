package br.com.educamais.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Metricas do fluxo autonomo (RNF02, RNF30). */
@Component
public class AttendanceMetrics {

    private final Counter absences;
    private final Counter duplicates;
    private final Counter plansFromLlm;
    private final Counter plansFromFallback;
    private final Counter whatsappFailures;
    private final Counter whatsappSkipped;
    private final Counter rewardsGranted;
    private final Counter reportsDelivered;
    private final Timer attendanceToNotification;

    public AttendanceMetrics(MeterRegistry registry) {
        this.absences = Counter.builder("educamais.attendance.ingested")
                .description("Faltas ingeridas com sucesso").register(registry);
        this.duplicates = Counter.builder("educamais.attendance.duplicate")
                .description("Eventos duplicados ignorados").register(registry);
        this.plansFromLlm = Counter.builder("educamais.agent.plan.llm")
                .description("Planos gerados pelo LLM").register(registry);
        this.plansFromFallback = Counter.builder("educamais.agent.plan.fallback")
                .description("Planos gerados pelo fallback deterministico").register(registry);
        this.whatsappFailures = Counter.builder("educamais.notification.whatsapp.failed")
                .description("Falhas de envio no WhatsApp").register(registry);
        this.whatsappSkipped = Counter.builder("educamais.notification.whatsapp.skipped")
                .description("Envios de WhatsApp ignorados").register(registry);
        this.rewardsGranted = Counter.builder("educamais.gamification.rewards")
                .description("Recompensas concedidas").register(registry);
        this.reportsDelivered = Counter.builder("educamais.report.delivered")
                .description("Relatorios entregues ao professor").register(registry);
        this.attendanceToNotification = Timer.builder("educamais.attendance.to_notification")
                .description("Tempo entre a falta e a notificacao (RNF02)").register(registry);
    }

    public void absenceIngested() {
        absences.increment();
    }

    public void duplicateEvent() {
        duplicates.increment();
    }

    public void planFromLlm() {
        plansFromLlm.increment();
    }

    public void planFromFallback() {
        plansFromFallback.increment();
    }

    public void whatsappFailure() {
        whatsappFailures.increment();
    }

    public void whatsappSkipped() {
        whatsappSkipped.increment();
    }

    public void rewardGranted() {
        rewardsGranted.increment();
    }

    public void reportDelivered() {
        reportsDelivered.increment();
    }

    public void recordAttendanceToNotification(long millis) {
        attendanceToNotification.record(millis, TimeUnit.MILLISECONDS);
    }
}
