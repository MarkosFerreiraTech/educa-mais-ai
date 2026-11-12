package br.com.educamais.domain.port;

import br.com.educamais.domain.model.Attendance;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository {

    Attendance save(Attendance attendance);

    Optional<Attendance> findById(UUID id);

    Optional<Attendance> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
