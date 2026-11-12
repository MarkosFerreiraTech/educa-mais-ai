package br.com.educamais.infrastructure.persistence.jpa;

import br.com.educamais.infrastructure.persistence.entity.StudentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, UUID> {

    Optional<StudentEntity> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);

    List<StudentEntity> findByClassroomId(UUID classroomId);
}
