package br.com.educamais.domain.port;

import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.RewardEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GamificationRepository {

    GamificationProfile save(GamificationProfile profile);

    Optional<GamificationProfile> findByStudentId(UUID studentId);

    /** RN03: garante recompensa unica por missao. */
    boolean existsRewardByKey(String idempotencyKey);

    RewardEntry saveReward(RewardEntry entry);

    List<RewardEntry> findRewardsByStudentId(UUID studentId);
}
