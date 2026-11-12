package br.com.educamais.agent.planner;

import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.Student;

/** Entrada do planejamento pedagogico (dados minimos observados pelo agente). */
public record RecoveryPlanRequest(Student student, Lesson lesson, InterestCode primaryInterest,
                                  String attendanceEventId) {

    public String interestHint() {
        return primaryInterest == null ? "cotidiano do aluno" : primaryInterest.contextHint();
    }

    public String firstName() {
        return student == null ? "Aluno" : student.firstName();
    }
}
