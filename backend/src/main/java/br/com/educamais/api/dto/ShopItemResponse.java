package br.com.educamais.api.dto;

public record ShopItemResponse(String code, String name, String category, int priceCoins, String assetUrl,
                               boolean owned) {
}
