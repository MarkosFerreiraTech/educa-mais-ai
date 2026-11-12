package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.Notification;
import br.com.educamais.domain.port.NotificationRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.entity.NotificationEntity;
import br.com.educamais.infrastructure.persistence.jpa.NotificationJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository repository;

    public NotificationPersistenceAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        // Reaproveita a entidade gerenciada quando ela ja existe no persistence context
        NotificationEntity entity = repository.findById(notification.id())
                .orElseGet(NotificationEntity::new);
        return EntityMapper.toDomain(repository.save(EntityMapper.toEntity(entity, notification)));
    }

    @Override
    public List<Notification> findByStudentId(UUID studentId) {
        return repository.findByStudentIdOrderBySentAtDesc(studentId).stream()
                .map(EntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findByMissionId(UUID missionId) {
        return repository.findByMissionId(missionId).stream().map(EntityMapper::toDomain).toList();
    }
}
