package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.RewardEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardJpaRepository extends JpaRepository<RewardEntity, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<RewardEntity> findByStudentId(UUID studentId);
}
