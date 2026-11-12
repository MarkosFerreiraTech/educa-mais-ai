package br.com.educamais.domain.model;

import java.util.List;

/** Ementa da aula perdida: ancora obrigatoria do plano do agente (RF15). */
public record Syllabus(List<String> learningObjectives, List<String> topics, List<String> prerequisites,
                       String sourceReference) {

    public Syllabus {
        learningObjectives = learningObjectives == null ? List.of() : List.copyOf(learningObjectives);
        topics = topics == null ? List.of() : List.copyOf(topics);
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    }

    public boolean hasObjectives() {
        return !learningObjectives.isEmpty();
    }

    public String objectivesAsText() {
        return String.join(" | ", learningObjectives);
    }

    public String topicsAsText() {
        return String.join(", ", topics);
    }
}
