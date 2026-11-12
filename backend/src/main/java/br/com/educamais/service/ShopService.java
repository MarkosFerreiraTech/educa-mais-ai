package br.com.educamais.service;

import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.port.GamificationRepository;
import br.com.educamais.domain.port.ShopRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loja de customizacao do avatar com Moedas (RF04, RF31 / RN01). */
@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final RewardService rewardService;
    private final GamificationRepository gamificationRepository;

    public ShopService(ShopRepository shopRepository, RewardService rewardService,
                       GamificationRepository gamificationRepository) {
        this.shopRepository = shopRepository;
        this.rewardService = rewardService;
        this.gamificationRepository = gamificationRepository;
    }

    public List<ShopItem> catalog() {
        return shopRepository.findAll();
    }

    /** Compra idempotente: item ja possuido nao e cobrado novamente. */
    @Transactional
    public PurchaseResult purchase(UUID studentId, String itemCode) {
        ShopItem item = shopRepository.findByCode(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemCode));
        if (shopRepository.isOwned(studentId, itemCode)) {
            return new PurchaseResult(item, rewardService.profileOf(studentId), false, 0);
        }
        GamificationProfile profile = rewardService.profileOf(studentId);
        if (!profile.spendCoins(item.priceCoins())) {
            throw new BusinessRuleException("INSUFFICIENT_COINS",
                    "Moedas insuficientes. Faltam %d moedas."
                            .formatted(item.priceCoins() - profile.coins()));
        }
        gamificationRepository.save(profile);
        shopRepository.registerOwnership(studentId, itemCode);
        return new PurchaseResult(item, profile, true, item.priceCoins());
    }

    public boolean isOwned(UUID studentId, String itemCode) {
        return shopRepository.isOwned(studentId, itemCode);
    }

    @Transactional
    public GamificationProfile convertDiamonds(UUID studentId, int amount) {
        return rewardService.convertDiamonds(studentId, amount);
    }

    /** Resultado da compra de item. */
    public record PurchaseResult(ShopItem item, GamificationProfile profile, boolean charged, int chargedCoins) {
    }
}
