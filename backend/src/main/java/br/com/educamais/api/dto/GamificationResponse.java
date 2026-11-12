package br.com.educamais.api.dto;

import java.util.UUID;

public record GamificationResponse(UUID studentId, int totalXp, int level, int xpToNextLevel, int diamonds,
                                   int coins, int currentStreak, int longestStreak) {
}
