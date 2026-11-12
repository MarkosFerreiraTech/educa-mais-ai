package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.ReportEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJpaRepository extends JpaRepository<ReportEntity, UUID> {

    Optional<ReportEntity> findByMissionId(UUID missionId);

    List<ReportEntity> findByStudentId(UUID studentId);

    List<ReportEntity> findByTeacherId(UUID teacherId);
}
