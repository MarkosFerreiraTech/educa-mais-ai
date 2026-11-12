package br.com.educamais.domain.port;

import br.com.educamais.domain.model.Lesson;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository {

    Lesson save(Lesson lesson);

    Optional<Lesson> findById(UUID id);
}
