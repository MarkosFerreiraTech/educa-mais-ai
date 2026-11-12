package br.com.educamais.domain.port;

import br.com.educamais.domain.model.ShopItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopRepository {

    List<ShopItem> findAll();

    Optional<ShopItem> findByCode(String code);

    boolean isOwned(UUID studentId, String itemCode);

    void registerOwnership(UUID studentId, String itemCode);
}
