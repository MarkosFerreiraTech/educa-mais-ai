package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.ShopItemEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopItemJpaRepository extends JpaRepository<ShopItemEntity, UUID> {

    Optional<ShopItemEntity> findByCode(String code);
}
