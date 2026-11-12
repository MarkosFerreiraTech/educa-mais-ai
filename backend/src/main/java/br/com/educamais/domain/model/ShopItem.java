package br.com.educamais.domain.model;

import java.util.UUID;

/** Item da loja de avatar, pago em Moedas (RF31). */
public record ShopItem(UUID id, String code, String name, String category, int priceCoins, String assetUrl) {

    public ShopItem {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Codigo do item e obrigatorio");
        }
        if (priceCoins < 0) {
            throw new IllegalArgumentException("Preco nao pode ser negativo");
        }
    }
}
