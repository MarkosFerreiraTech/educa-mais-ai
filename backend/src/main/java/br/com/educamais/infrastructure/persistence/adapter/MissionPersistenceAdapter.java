package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.port.MissionRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.entity.MissionEntity;
import br.com.educamais.infrastructure.persistence.jpa.MissionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MissionPersistenceAdapter implements MissionRepository {

    private final MissionJpaRepository repository;

    public MissionPersistenceAdapter(MissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mission save(Mission mission) {
        // Reaproveita a entidade gerenciada quando ela ja existe no persistence context
        MissionEntity entity = repository.findById(mission.id())
                .orElseGet(MissionEntity::new);
        return EntityMapper.toDomain(repository.save(EntityMapper.toEntity(entity, mission)));
    }

    @Override
    public Optional<Mission> findById(UUID id) {
        return repository.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Mission> findByAttendanceId(UUID attendanceId) {
        return repository.findByAttendanceId(attendanceId).map(EntityMapper::toDomain);
    }

    @Override
    public List<Mission> findByStudentId(UUID studentId) {
        return repository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(EntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Mission> findByLessonId(UUID lessonId) {
        return repository.findByLessonId(lessonId).stream().map(EntityMapper::toDomain).toList();
    }
}
