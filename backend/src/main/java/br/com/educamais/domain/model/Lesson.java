package br.com.educamais.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/** Aula do calendario escolar e sua ementa. */
public record Lesson(UUID id, UUID classroomId, String subject, String topic, LocalDate lessonDate,
                     Syllabus syllabus) {

    public Lesson {
        if (lessonDate == null) {
            lessonDate = LocalDate.now();
        }
        syllabus = syllabus == null ? new Syllabus(null, null, null, null) : syllabus;
    }

    public boolean hasSyllabus() {
        return syllabus.hasObjectives();
    }

    public String reference() {
        return "%s - %s (%s)".formatted(subject, topic, lessonDate);
    }
}
