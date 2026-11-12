package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.AuditEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditJpaRepository extends JpaRepository<AuditEntity, UUID> {

    List<AuditEntity> findByMissionIdOrderByCreatedAtAsc(UUID missionId);
}
