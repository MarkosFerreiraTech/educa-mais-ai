package br.com.educamais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.model.GamificationProfile;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RN01, RN03, RN04, RF29-RF32. */
class GamificationProfileTest {

    private final UUID studentId = UUID.randomUUID();

    @Test
    @DisplayName("Recompensa concede 50 XP e 1 diamante")
    void recompensaPadrao() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        profile.grantMissionReward();
        assertThat(profile.totalXp()).isEqualTo(50);
        assertThat(profile.diamonds()).isEqualTo(1);
        assertThat(profile.level()).isEqualTo(1);
        assertThat(profile.xpToNextLevel()).isEqualTo(50);
    }

    @Test
    @DisplayName("1 diamante vale 10 moedas (RN01)")
    void converteDiamante() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        profile.grantMissionReward();
        int coins = profile.convertDiamondsToCoins(1);
        assertThat(coins).isEqualTo(10);
        assertThat(profile.coins()).isEqualTo(10);
        assertThat(profile.diamonds()).isZero();
    }

    @Test
    @DisplayName("Nao converte mais diamantes do que possui")
    void diamantesInsuficientes() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        assertThatThrownBy(() -> profile.convertDiamondsToCoins(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Compra sem saldo nao debita nada")
    void saldoInsuficiente() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        assertThat(profile.spendCoins(20)).isFalse();
        assertThat(profile.coins()).isZero();
    }

    @Test
    @DisplayName("Ofensiva incrementa em dias consecutivos (RN04)")
    void ofensivaConsecutiva() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        LocalDate day = LocalDate.of(2026, 11, 10);
        profile.registerActiveDay(day);
        profile.registerActiveDay(day.plusDays(1));
        profile.registerActiveDay(day.plusDays(2));
        assertThat(profile.currentStreak()).isEqualTo(3);
        assertThat(profile.longestStreak()).isEqualTo(3);
    }

    @Test
    @DisplayName("Ofensiva reinicia quando ha dia pulado")
    void ofensivaQuebrada() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        LocalDate day = LocalDate.of(2026, 11, 10);
        profile.registerActiveDay(day);
        profile.registerActiveDay(day.plusDays(1));
        profile.registerActiveDay(day.plusDays(5));
        assertThat(profile.currentStreak()).isEqualTo(1);
        assertThat(profile.longestStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("Mesmo dia nao duplica a ofensiva")
    void ofensivaIdempotente() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        LocalDate day = LocalDate.of(2026, 11, 10);
        profile.registerActiveDay(day);
        profile.registerActiveDay(day);
        assertThat(profile.currentStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("Nivel pela curva 100 x nivel (RF32)")
    void calculoDeNivel() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        for (int index = 0; index < 4; index++) {
            profile.grantMissionReward();
        }
        assertThat(profile.totalXp()).isEqualTo(200);
        assertThat(profile.level()).isEqualTo(3);
    }

    @Test
    @DisplayName("Reset zera apenas a ofensiva atual")
    void resetDeOfensiva() {
        GamificationProfile profile = GamificationProfile.newFor(studentId);
        profile.registerActiveDay(LocalDate.of(2026, 11, 10));
        profile.resetStreak();
        assertThat(profile.currentStreak()).isZero();
        assertThat(profile.longestStreak()).isEqualTo(1);
    }
}
