package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.RewardEntry;
import br.com.educamais.domain.port.GamificationRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.jpa.GamificationJpaRepository;
import br.com.educamais.infrastructure.persistence.jpa.RewardJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GamificationPersistenceAdapter implements GamificationRepository {

    private final GamificationJpaRepository profileRepository;
    private final RewardJpaRepository rewardRepository;

    public GamificationPersistenceAdapter(GamificationJpaRepository profileRepository,
                                          RewardJpaRepository rewardRepository) {
        this.profileRepository = profileRepository;
        this.rewardRepository = rewardRepository;
    }

    @Override
    public GamificationProfile save(GamificationProfile profile) {
        return EntityMapper.toDomain(profileRepository.save(EntityMapper.toEntity(profile)));
    }

    @Override
    public Optional<GamificationProfile> findByStudentId(UUID studentId) {
        return profileRepository.findById(studentId).map(EntityMapper::toDomain);
    }

    @Override
    public boolean existsRewardByKey(String idempotencyKey) {
        return idempotencyKey != null && rewardRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public RewardEntry saveReward(RewardEntry entry) {
        return EntityMapper.toDomain(rewardRepository.save(EntityMapper.toEntity(entry)));
    }

    @Override
    public List<RewardEntry> findRewardsByStudentId(UUID studentId) {
        return rewardRepository.findByStudentId(studentId).stream().map(EntityMapper::toDomain).toList();
    }
}
