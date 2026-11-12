package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.crypto.CryptoSupport;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.StudentRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.jpa.StudentJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StudentPersistenceAdapter implements StudentRepository {

    private final StudentJpaRepository repository;

    public StudentPersistenceAdapter(StudentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Student save(Student student) {
        return EntityMapper.toDomain(repository.save(EntityMapper.toEntity(student)));
    }

    @Override
    public Optional<Student> findById(UUID id) {
        return repository.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String hash = CryptoSupport.encryptor().blindIndex(email);
        return repository.findByEmailHash(hash).map(EntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && repository.existsByEmailHash(CryptoSupport.encryptor().blindIndex(email));
    }

    @Override
    public List<Student> findByClassroomId(UUID classroomId) {
        return repository.findByClassroomId(classroomId).stream().map(EntityMapper::toDomain).toList();
    }
}
