package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.GamificationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamificationJpaRepository extends JpaRepository<GamificationEntity, UUID> {
}
