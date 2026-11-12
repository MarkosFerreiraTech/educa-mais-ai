package br.com.educamais.api.controller;

import br.com.educamais.api.dto.ConvertDiamondsRequest;
import br.com.educamais.api.dto.GamificationResponse;
import br.com.educamais.api.dto.PurchaseRequest;
import br.com.educamais.api.dto.ShopItemResponse;
import br.com.educamais.api.mapper.ApiMapper;
import br.com.educamais.security.AuthenticatedPrincipal;
import br.com.educamais.service.ShopService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Loja de customizacao do avatar (RF04, RF31 / UC02). */
@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/items")
    public List<ShopItemResponse> items(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return shopService.catalog().stream()
                .map(item -> ApiMapper.toShopItem(item,
                        shopService.isOwned(principal.subjectId(), item.code())))
                .toList();
    }

    @PostMapping("/purchase")
    public GamificationResponse purchase(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                         @Valid @RequestBody PurchaseRequest request) {
        ShopService.PurchaseResult result = shopService.purchase(principal.subjectId(), request.itemCode());
        return ApiMapper.toGamification(result.profile());
    }

    @PostMapping("/diamonds/convert")
    public GamificationResponse convert(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                        @Valid @RequestBody ConvertDiamondsRequest request) {
        UUID studentId = principal.subjectId();
        return ApiMapper.toGamification(shopService.convertDiamonds(studentId, request.amount()));
    }
}
