package br.com.educamais.domain.port;

import br.com.educamais.domain.model.TeacherReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {

    TeacherReport save(TeacherReport report);

    Optional<TeacherReport> findByMissionId(UUID missionId);

    List<TeacherReport> findByStudentId(UUID studentId);
}
