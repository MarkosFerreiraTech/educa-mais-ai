package br.com.educamais.domain.port;

import br.com.educamais.domain.model.Mission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository {

    Mission save(Mission mission);

    Optional<Mission> findById(UUID id);

    Optional<Mission> findByAttendanceId(UUID attendanceId);

    List<Mission> findByStudentId(UUID studentId);

    List<Mission> findByLessonId(UUID lessonId);
}
