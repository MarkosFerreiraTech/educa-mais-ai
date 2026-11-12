package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.MissionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionJpaRepository extends JpaRepository<MissionEntity, UUID> {

    Optional<MissionEntity> findByAttendanceId(UUID attendanceId);

    List<MissionEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<MissionEntity> findByLessonId(UUID lessonId);
}
