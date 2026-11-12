package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.AttendanceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceJpaRepository extends JpaRepository<AttendanceEntity, UUID> {

    Optional<AttendanceEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<AttendanceEntity> findByStudentIdOrderByOccurredAtDesc(UUID studentId);

    List<AttendanceEntity> findByLessonId(UUID lessonId);
}
