package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.LessonEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonJpaRepository extends JpaRepository<LessonEntity, UUID> {
}
