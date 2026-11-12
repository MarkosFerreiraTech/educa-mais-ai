package br.com.educamais.service;

import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.event.MissionCompletedEvent;
import br.com.educamais.domain.exception.AccessDeniedException;
import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionStep;
import br.com.educamais.domain.port.MissionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta e acesso as missoes de recuperacao (RF27, RF28). */
@Service
public class MissionService {

    private final MissionRepository missionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MissionService(MissionRepository missionRepository, ApplicationEventPublisher eventPublisher) {
        this.missionRepository = missionRepository;
        this.eventPublisher = eventPublisher;
    }

    /** RF28: somente o dono da missao acessa seus dados. */
    public Mission getForStudent(UUID missionId, UUID studentId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Missao", missionId));
        if (!mission.studentId().equals(studentId)) {
            throw new AccessDeniedException("Missao pertence a outro aluno");
        }
        return mission;
    }

    public List<Mission> listForStudent(UUID studentId) {
        return missionRepository.findByStudentId(studentId);
    }

    @Transactional
    public Mission start(UUID missionId, UUID studentId) {
        Mission mission = getForStudent(missionId, studentId);
        if (mission.status() == MissionStatus.CANCELLED || mission.status() == MissionStatus.ABANDONED) {
            throw new BusinessRuleException("MISSION_NOT_STARTABLE", "Missao nao pode ser iniciada");
        }
        if (!mission.status().isFinished()) {
            // RN02: o aluno so inicia uma missao que ja lhe foi notificada.
            if (mission.status() == MissionStatus.PLANNED) {
                mission.markNotified();
            }
            mission.start();
            mission = missionRepository.save(mission);
        }
        return mission;
    }

    @Transactional
    public Mission save(Mission mission) {
        Mission saved = missionRepository.save(mission);
        if (saved.isFinished()) {
            eventPublisher.publishEvent(new MissionCompletedEvent(saved.id(), saved.studentId(),
                    saved.lessonId(), saved.status() == MissionStatus.COMPLETED_WITH_SUPPORT));
        }
        return saved;
    }

    public MissionStep currentStep(Mission mission) {
        return mission.currentStep().orElseThrow(
                () -> new BusinessRuleException("MISSION_FINISHED", "Missao sem passos pendentes"));
    }

    public Mission get(UUID missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Missao", missionId));
    }
}
