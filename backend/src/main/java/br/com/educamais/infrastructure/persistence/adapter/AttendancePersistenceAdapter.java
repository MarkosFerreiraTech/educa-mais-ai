package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.Attendance;
import br.com.educamais.domain.port.AttendanceRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.jpa.AttendanceJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AttendancePersistenceAdapter implements AttendanceRepository {

    private final AttendanceJpaRepository repository;

    public AttendancePersistenceAdapter(AttendanceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Attendance save(Attendance attendance) {
        return EntityMapper.toDomain(repository.save(EntityMapper.toEntity(attendance)));
    }

    @Override
    public Optional<Attendance> findById(UUID id) {
        return repository.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Attendance> findByEventId(String eventId) {
        return repository.findByEventId(eventId).map(EntityMapper::toDomain);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return eventId != null && repository.existsByEventId(eventId);
    }
}
