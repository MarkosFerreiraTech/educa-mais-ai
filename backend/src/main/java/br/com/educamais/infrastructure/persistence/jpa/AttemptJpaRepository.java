package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.AttemptEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptJpaRepository extends JpaRepository<AttemptEntity, UUID> {

    List<AttemptEntity> findByStudentId(UUID studentId);

    List<AttemptEntity> findByQuestionId(UUID questionId);
}
