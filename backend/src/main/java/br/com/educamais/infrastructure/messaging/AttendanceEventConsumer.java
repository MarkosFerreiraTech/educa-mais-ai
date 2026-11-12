package br.com.educamais.infrastructure.messaging;

import br.com.educamais.agent.MissionOrchestrator;
import br.com.educamais.domain.event.AttendanceRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumidor do evento de falta (Observer / RF10).
 *
 * <p>Em nuvem o evento chega via SQS/EventBridge; localmente, o listener transacional faz o papel
 * da fila, disparando o agente em thread separada ({@code @Async}) apos o commit da falta.</p>
 */
@Component
public class AttendanceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AttendanceEventConsumer.class);

    private final MissionOrchestrator orchestrator;

    public AttendanceEventConsumer(MissionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async("agentExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AttendanceRegisteredEvent event) {
        try {
            log.info("ATTENDANCE_EVENT_RECEIVED attendanceRef={} studentRef={}", event.attendanceId(),
                    event.studentId());
            orchestrator.runForAttendance(event.attendanceId());
        } catch (Exception exception) {
            log.error("ATTENDANCE_EVENT_FAILED attendanceRef={} reason={}", event.attendanceId(),
                    exception.getMessage());
        }
    }
}
