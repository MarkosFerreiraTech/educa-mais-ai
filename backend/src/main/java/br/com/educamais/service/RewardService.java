package br.com.educamais.service;

import br.com.educamais.domain.enums.RewardType;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.RewardEntry;
import br.com.educamais.domain.port.GamificationRepository;
import br.com.educamais.domain.port.ClockPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gamificacao da recuperacao (RF29-RF32 / RN01-RN04).
 * Recompensa idempotente por missao e ofensiva de dias letivos.
 */
@Service
public class RewardService {

    private final GamificationRepository gamificationRepository;
    private final ClockPort clockPort;
    private final AttendanceMetrics metrics;

    public RewardService(GamificationRepository gamificationRepository, ClockPort clockPort,
                         AttendanceMetrics metrics) {
        this.gamificationRepository = gamificationRepository;
        this.clockPort = clockPort;
        this.metrics = metrics;
    }

    public GamificationProfile profileOf(java.util.UUID studentId) {
        return gamificationRepository.findByStudentId(studentId)
                .orElseGet(() -> gamificationRepository.save(GamificationProfile.newFor(studentId)));
    }

    /** RN03: concede XP e Diamante uma unica vez por missao. */
    @Transactional
    public RewardGrant grantMissionReward(Mission mission, String attendanceEventId) {
        if (!mission.isFinished()) {
            throw new br.com.educamais.domain.exception.BusinessRuleException("MISSION_NOT_FINISHED",
                    "Recompensa somente apos a conclusao da missao");
        }
        String rewardKey = attendanceEventId == null || attendanceEventId.isBlank()
                ? mission.id().toString() : attendanceEventId;
        String key = "%s:%s:%s".formatted(RewardType.XP, rewardKey, mission.studentId());
        GamificationProfile profile = profileOf(mission.studentId());
        if (gamificationRepository.existsRewardByKey(key)) {
            return new RewardGrant(profile, false, GamificationProfile.XP_PER_MISSION,
                    GamificationProfile.DIAMONDS_PER_MISSION);
        }
        profile.grantMissionReward();
        profile.registerActiveDay(clockPort.today());
        gamificationRepository.saveReward(RewardEntry.of(mission.studentId(), mission.id(), RewardType.XP,
                GamificationProfile.XP_PER_MISSION));
        gamificationRepository.saveReward(RewardEntry.of(mission.studentId(), mission.id(), RewardType.DIAMOND,
                GamificationProfile.DIAMONDS_PER_MISSION));
        GamificationProfile saved = gamificationRepository.save(profile);
        metrics.rewardGranted();
        return new RewardGrant(saved, true, GamificationProfile.XP_PER_MISSION,
                GamificationProfile.DIAMONDS_PER_MISSION);
    }

    /** RF31/RN01: converte diamantes em moedas. */
    @Transactional
    public GamificationProfile convertDiamonds(java.util.UUID studentId, int amount) {
        GamificationProfile profile = profileOf(studentId);
        profile.convertDiamondsToCoins(amount);
        return gamificationRepository.save(profile);
    }

    /** RF30: ofensiva mantida ao cumprir o dia letivo sem falta pendente. */
    @Transactional
    public GamificationProfile registerStreakDay(java.util.UUID studentId) {
        GamificationProfile profile = profileOf(studentId);
        profile.registerActiveDay(clockPort.today());
        return gamificationRepository.save(profile);
    }

    @Transactional
    public GamificationProfile resetStreak(java.util.UUID studentId) {
        GamificationProfile profile = profileOf(studentId);
        profile.resetStreak();
        return gamificationRepository.save(profile);
    }

    public List<RewardEntry> history(java.util.UUID studentId) {
        return gamificationRepository.findRewardsByStudentId(studentId);
    }

    /** Resultado da concessao de recompensa. */
    public record RewardGrant(GamificationProfile profile, boolean newlyGranted, int xpGranted,
                              int diamondsGranted) {
    }
}
