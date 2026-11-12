package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.port.ClockPort;
import br.com.educamais.domain.port.GamificationRepository;
import br.com.educamais.domain.port.ShopRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** RF04, RF31 / UC02: compra de itens com Moedas. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;
    @Mock
    private GamificationRepository gamificationRepository;

    private ShopService service;
    private final UUID studentId = UUID.randomUUID();
    private final ShopItem shirt = new ShopItem(UUID.randomUUID(), "camisa-time", "Camisa do Time", "OUTFIT", 20,
            "/assets/shirt.png");

    @BeforeEach
    void setUp() {
        ClockPort clock = new ClockPort() {
            @Override
            public java.time.Instant now() {
                return java.time.Instant.parse("2026-11-12T10:00:00Z");
            }

            @Override
            public LocalDate today() {
                return LocalDate.of(2026, 11, 12);
            }
        };
        RewardService rewardService = new RewardService(gamificationRepository, clock,
                new AttendanceMetrics(new SimpleMeterRegistry()));
        service = new ShopService(shopRepository, rewardService, gamificationRepository);

        when(shopRepository.findByCode("camisa-time")).thenReturn(Optional.of(shirt));
        when(shopRepository.findAll()).thenReturn(List.of(shirt));
        when(gamificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private GamificationProfile richProfile() {
        GamificationProfile profile = new GamificationProfile(studentId, 0, 5, 0, 0, 0, null);
        profile.convertDiamondsToCoins(4);
        return profile;
    }

    @Test
    @DisplayName("Compra com saldo suficiente debita moedas e registra posse")
    void compraComSaldo() {
        when(gamificationRepository.findByStudentId(studentId)).thenReturn(Optional.of(richProfile()));
        when(shopRepository.isOwned(studentId, "camisa-time")).thenReturn(false);

        ShopService.PurchaseResult result = service.purchase(studentId, "camisa-time");

        assertThat(result.charged()).isTrue();
        assertThat(result.chargedCoins()).isEqualTo(20);
        assertThat(result.profile().coins()).isEqualTo(20);
        verify(shopRepository).registerOwnership(studentId, "camisa-time");
    }

    @Test
    @DisplayName("Saldo insuficiente retorna INSUFFICIENT_COINS sem debitar")
    void saldoInsuficiente() {
        when(gamificationRepository.findByStudentId(studentId))
                .thenReturn(Optional.of(GamificationProfile.newFor(studentId)));
        when(shopRepository.isOwned(studentId, "camisa-time")).thenReturn(false);

        assertThatThrownBy(() -> service.purchase(studentId, "camisa-time"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_COINS");
        verify(shopRepository, never()).registerOwnership(any(), any());
    }

    @Test
    @DisplayName("Item ja possuido nao e cobrado novamente")
    void itemJaPossuido() {
        when(shopRepository.isOwned(studentId, "camisa-time")).thenReturn(true);
        when(gamificationRepository.findByStudentId(studentId)).thenReturn(Optional.of(richProfile()));

        ShopService.PurchaseResult result = service.purchase(studentId, "camisa-time");

        assertThat(result.charged()).isFalse();
        verify(shopRepository, never()).registerOwnership(any(), any());
    }
}
