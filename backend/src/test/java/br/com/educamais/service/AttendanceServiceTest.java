package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.event.AttendanceRegisteredEvent;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.Attendance;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.Syllabus;
import br.com.educamais.domain.port.AttendanceRepository;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.StudentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/** RF06-RF11: ingestao idempotente e publicacao do evento (Observer). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AttendanceService service;
    private final UUID studentId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AttendanceService(attendanceRepository, studentRepository, lessonRepository,
                eventPublisher, new AttendanceMetrics(new SimpleMeterRegistry()));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(Student.register("Ana", "a@a.com",
                null, null, "hash", null, null, true)));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(new Lesson(lessonId, null, "Matematica",
                "Fracoes", null, new Syllabus(List.of("Comparar fracoes"), List.of("fracoes"), List.of(), "BNEF"))));
        when(attendanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Falta nova e persistida e o evento e publicado")
    void registraFalta() {
        when(attendanceRepository.existsByEventId("evt-1")).thenReturn(false);

        Optional<Attendance> attendance = service.registerAbsence(studentId, lessonId, "evt-1", Instant.now());

        assertThat(attendance).isPresent();
        assertThat(attendance.get().eventId()).isEqualTo("evt-1");
        verify(eventPublisher).publishEvent(any(AttendanceRegisteredEvent.class));
    }

    @Test
    @DisplayName("Evento duplicado nao cria nova falta nem publica evento (RF09)")
    void idempotencia() {
        when(attendanceRepository.existsByEventId("evt-1")).thenReturn(true);

        Optional<Attendance> attendance = service.registerAbsence(studentId, lessonId, "evt-1", Instant.now());

        assertThat(attendance).isEmpty();
        verify(attendanceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Aluno inexistente gera erro de recurso")
    void alunoInexistente() {
        when(attendanceRepository.existsByEventId("evt-2")).thenReturn(false);
        UUID unknown = UUID.randomUUID();
        when(studentRepository.findById(unknown)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.registerAbsence(unknown, lessonId, "evt-2", Instant.now()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Justificar falta altera o status")
    void justificar() {
        UUID attendanceId = UUID.randomUUID();
        Attendance attendance = Attendance.fromWebhook(studentId, lessonId, "evt-3", Instant.now());
        when(attendanceRepository.findById(attendanceId)).thenReturn(Optional.of(attendance));

        Attendance justified = service.justify(attendanceId);

        assertThat(justified.status().name()).isEqualTo("JUSTIFIED");
        assertThat(justified.isJustified()).isTrue();
    }
}
