package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.RewardType;
import java.time.Instant;
import java.util.UUID;

/** Lancamento do razao de recompensas -- idempotente por chave (RN03). */
public record RewardEntry(UUID id, UUID studentId, UUID missionId, RewardType type, int amount,
                          String idempotencyKey, Instant grantedAt) {

    public RewardEntry {
        id = id == null ? UUID.randomUUID() : id;
        grantedAt = grantedAt == null ? Instant.now() : grantedAt;
    }

    public static RewardEntry of(UUID studentId, UUID missionId, RewardType type, int amount) {
        return new RewardEntry(null, studentId, missionId, type, amount,
                "%s:%s:%s".formatted(type, missionId, studentId), Instant.now());
    }
}
