package br.com.educamais.domain.port;

import br.com.educamais.domain.model.Student;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistencia de alunos (DIP). */
public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(UUID id);

    Optional<Student> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Student> findByClassroomId(UUID classroomId);
}
