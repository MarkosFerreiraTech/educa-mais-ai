package br.com.educamais.service;

import br.com.educamais.domain.enums.AttendanceStatus;
import br.com.educamais.domain.event.AttendanceRegisteredEvent;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.Attendance;
import br.com.educamais.domain.port.AttendanceRepository;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingestao de faltas (RF09-RF11, RF06).
 * A publicacao do evento de dominio aciona o agente de forma desacoplada (Observer).
 */
@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttendanceMetrics metrics;

    public AttendanceService(AttendanceRepository attendanceRepository, StudentRepository studentRepository,
                             LessonRepository lessonRepository, ApplicationEventPublisher eventPublisher,
                             AttendanceMetrics metrics) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.lessonRepository = lessonRepository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    /**
     * Registra a falta de forma idempotente e publica o evento para o agente.
     *
     * @return {@code empty} quando o evento ja havia sido processado (RF09)
     */
    @Transactional
    public Optional<Attendance> registerAbsence(UUID studentId, UUID lessonId, String eventId, Instant occurredAt) {
        if (attendanceRepository.existsByEventId(eventId)) {
            log.info("ATTENDANCE_DUPLICATE_EVENT eventRef={}", eventId);
            metrics.duplicateEvent();
            return Optional.empty();
        }
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentId));
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula", lessonId));

        Attendance attendance = attendanceRepository.save(
                Attendance.fromWebhook(studentId, lessonId, eventId, occurredAt));
        metrics.absenceIngested();
        log.info("ATTENDANCE_REGISTERED attendanceRef={} studentRef={}", attendance.id(), studentId);
        eventPublisher.publishEvent(new AttendanceRegisteredEvent(attendance.id(), studentId, lessonId,
                eventId, attendance.occurredAt()));
        return Optional.of(attendance);
    }

    @Transactional
    public Attendance justify(UUID attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Falta", attendanceId));
        return attendanceRepository.save(attendance.justify());
    }

    public Attendance get(UUID attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Falta", attendanceId));
    }

    public boolean isAbsent(UUID attendanceId) {
        return get(attendanceId).status() == AttendanceStatus.ABSENT;
    }
}
