package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.port.ShopRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.entity.OwnedItemEntity;
import br.com.educamais.infrastructure.persistence.entity.ShopItemEntity;
import br.com.educamais.infrastructure.persistence.jpa.OwnedItemJpaRepository;
import br.com.educamais.infrastructure.persistence.jpa.ShopItemJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShopPersistenceAdapter implements ShopRepository {

    private final ShopItemJpaRepository itemRepository;
    private final OwnedItemJpaRepository ownedRepository;

    public ShopPersistenceAdapter(ShopItemJpaRepository itemRepository,
                                  OwnedItemJpaRepository ownedRepository) {
        this.itemRepository = itemRepository;
        this.ownedRepository = ownedRepository;
    }

    @Override
    public ShopItem save(ShopItem item) {
        ShopItemEntity entity = item.id() == null
                ? new ShopItemEntity()
                : itemRepository.findById(item.id()).orElseGet(ShopItemEntity::new);
        return EntityMapper.toDomain(itemRepository.save(EntityMapper.toEntity(entity, item)));
    }

    @Override
    public List<ShopItem> findAll() {
        return itemRepository.findAll().stream().map(EntityMapper::toDomain).toList();
    }

    @Override
    public Optional<ShopItem> findByCode(String code) {
        return itemRepository.findByCode(code).map(EntityMapper::toDomain);
    }

    @Override
    public boolean isOwned(UUID studentId, String itemCode) {
        return ownedRepository.existsByStudentIdAndItemCode(studentId, itemCode);
    }

    @Override
    public void registerOwnership(UUID studentId, String itemCode) {
        if (isOwned(studentId, itemCode)) {
            return;
        }
        OwnedItemEntity entity = new OwnedItemEntity();
        entity.setId(UUID.randomUUID());
        entity.setStudentId(studentId);
        entity.setItemCode(itemCode);
        entity.setAcquiredAt(Instant.now());
        ownedRepository.save(entity);
    }
}
