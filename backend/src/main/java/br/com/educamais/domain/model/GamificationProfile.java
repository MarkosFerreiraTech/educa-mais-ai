package br.com.educamais.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Perfil de gamificacao do aluno (RF29-RF32 / RN01, RN03, RN04).
 */
public class GamificationProfile {

    public static final int XP_PER_MISSION = 50;
    public static final int DIAMONDS_PER_MISSION = 1;
    public static final int COINS_PER_DIAMOND = 10;
    public static final int XP_BASE_PER_LEVEL = 100;

    private final UUID studentId;
    private int totalXp;
    private int diamonds;
    private int coins;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastStreakDate;

    public GamificationProfile(UUID studentId, int totalXp, int diamonds, int coins,
                               int currentStreak, int longestStreak, LocalDate lastStreakDate) {
        this.studentId = studentId;
        this.totalXp = Math.max(totalXp, 0);
        this.diamonds = Math.max(diamonds, 0);
        this.coins = Math.max(coins, 0);
        this.currentStreak = Math.max(currentStreak, 0);
        this.longestStreak = Math.max(longestStreak, 0);
        this.lastStreakDate = lastStreakDate;
    }

    public static GamificationProfile newFor(UUID studentId) {
        return new GamificationProfile(studentId, 0, 0, 0, 0, 0, null);
    }

    /** RF29: concede a recompensa padrao da missao concluida. */
    public void grantMissionReward() {
        this.totalXp += XP_PER_MISSION;
        this.diamonds += DIAMONDS_PER_MISSION;
    }

    /** RF31/RN01: converte diamantes em moedas (1 diamante = 10 moedas). */
    public int convertDiamondsToCoins(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Quantidade de diamantes deve ser positiva");
        }
        if (amount > diamonds) {
            throw new IllegalStateException("Diamantes insuficientes");
        }
        int gained = amount * COINS_PER_DIAMOND;
        this.diamonds -= amount;
        this.coins += gained;
        return gained;
    }

    public boolean canAfford(int price) {
        return price >= 0 && coins >= price;
    }

    /** RF31: debita moedas; retorna false (sem alteracao) se o saldo for insuficiente. */
    public boolean spendCoins(int price) {
        if (!canAfford(price)) {
            return false;
        }
        this.coins -= price;
        return true;
    }

    /** RF30/RN04: mantem a ofensiva de dias letivos consecutivos. */
    public void registerActiveDay(LocalDate day) {
        if (day == null) {
            throw new IllegalArgumentException("Dia letivo e obrigatorio");
        }
        if (lastStreakDate != null && !day.isAfter(lastStreakDate)) {
            return;
        }
        boolean consecutive = lastStreakDate != null && day.equals(lastStreakDate.plusDays(1));
        currentStreak = consecutive ? currentStreak + 1 : 1;
        longestStreak = Math.max(longestStreak, currentStreak);
        lastStreakDate = day;
    }

    public void resetStreak() {
        this.currentStreak = 0;
    }

    /** RF32: nivel pela curva 100 x nivel. */
    public int level() {
        return Math.max(1, totalXp / XP_BASE_PER_LEVEL + 1);
    }

    public int xpToNextLevel() {
        return level() * XP_BASE_PER_LEVEL - totalXp;
    }

    public UUID studentId() {
        return studentId;
    }

    public int totalXp() {
        return totalXp;
    }

    public int diamonds() {
        return diamonds;
    }

    public int coins() {
        return coins;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public int longestStreak() {
        return longestStreak;
    }

    public LocalDate lastStreakDate() {
        return lastStreakDate;
    }
}
