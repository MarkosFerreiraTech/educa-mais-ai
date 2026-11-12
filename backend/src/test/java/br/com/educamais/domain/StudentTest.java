package br.com.educamais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.enums.StudentStatus;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Student;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF01, RF03, RF14, RF37, RF38. */
class StudentTest {

    private Student newStudent() {
        return Student.register("Ana Souza", "ANA@Escola.br", "+5581999998888", LocalDate.of(2012, 4, 10),
                "hash", null, null, true);
    }

    @Test
    @DisplayName("Cadastro normaliza e-mail e inicia em PENDING_ONBOARDING")
    void cadastro() {
        Student student = newStudent();
        assertThat(student.email()).isEqualTo("ana@escola.br");
        assertThat(student.status()).isEqualTo(StudentStatus.PENDING_ONBOARDING);
        assertThat(student.firstName()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Interesses definem o onboarding como ACTIVE e priorizam o primeiro")
    void interesses() {
        Student student = newStudent();
        student.defineInterests(List.of(Interest.of(InterestCode.FOOTBALL, 5), Interest.of(InterestCode.GAMES, 3)));
        assertThat(student.status()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(student.primaryInterest()).contains(InterestCode.FOOTBALL);
        assertThat(student.interests()).hasSize(2);
    }

    @Test
    @DisplayName("Sem interesses, aplica interesse padrao")
    void interessePadrao() {
        Student student = newStudent();
        student.defineInterests(List.of());
        assertThat(student.primaryInterest()).contains(InterestCode.NATURE);
    }

    @Test
    @DisplayName("Interesses duplicados sao ignorados")
    void interessesDuplicados() {
        Student student = newStudent();
        student.defineInterests(List.of(Interest.of(InterestCode.GAMES, 5), Interest.of(InterestCode.GAMES, 4)));
        assertThat(student.interests()).hasSize(1);
    }

    @Test
    @DisplayName("E-mail invalido e rejeitado")
    void emailInvalido() {
        assertThatThrownBy(() -> Student.register("Ana", "ana-escola", null, null, null, null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Anonimizacao remove PII e muda o status (RF38)")
    void anonimizacao() {
        Student student = newStudent();
        student.anonymize();
        assertThat(student.name()).isEqualTo("Aluno Anonimizado");
        assertThat(student.phone()).isNull();
        assertThat(student.birthDate()).isNull();
        assertThat(student.status()).isEqualTo(StudentStatus.ANONYMIZED);
        assertThat(student.email()).contains("anonimo+");
    }
}
