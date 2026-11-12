package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.OwnedItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedItemJpaRepository extends JpaRepository<OwnedItemEntity, UUID> {

    boolean existsByStudentIdAndItemCode(UUID studentId, String itemCode);

    List<OwnedItemEntity> findByStudentId(UUID studentId);
}
