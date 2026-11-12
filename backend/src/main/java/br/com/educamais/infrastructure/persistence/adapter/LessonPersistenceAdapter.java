package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.entity.LessonEntity;
import br.com.educamais.infrastructure.persistence.jpa.LessonJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LessonPersistenceAdapter implements LessonRepository {

    private final LessonJpaRepository repository;

    public LessonPersistenceAdapter(LessonJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Lesson save(Lesson lesson) {
        // Reaproveita a entidade gerenciada quando ela ja existe no persistence context
        LessonEntity entity = repository.findById(lesson.id())
                .orElseGet(LessonEntity::new);
        return EntityMapper.toDomain(repository.save(EntityMapper.toEntity(entity, lesson)));
    }

    @Override
    public Optional<Lesson> findById(UUID id) {
        return repository.findById(id).map(EntityMapper::toDomain);
    }
}
