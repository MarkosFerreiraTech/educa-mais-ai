package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.GamificationRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** RF05, RF32: perfil do aluno, preferencias e criacao preguicosa da gamificacao. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GamificationRepository gamificationRepository;

    private StudentService service;
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentService(studentRepository, gamificationRepository);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gamificationRepository.save(any(GamificationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Student student() {
        return Student.register("Ana Souza", "ana@escola.br", "+5581999998888", null, "hash", null, null, true);
    }

    @Test
    @DisplayName("aluno inexistente gera 404")
    void alunoInexistente() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(studentId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("perfil de gamificacao e criado na primeira consulta")
    void criaPerfilDeGamificacao() {
        when(gamificationRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        GamificationProfile profile = service.profileOf(studentId);

        assertThat(profile.studentId()).isEqualTo(studentId);
        assertThat(profile.totalXp()).isZero();
        verify(gamificationRepository).save(any(GamificationProfile.class));
    }

    @Test
    @DisplayName("perfil existente e reutilizado")
    void reutilizaPerfilExistente() {
        GamificationProfile existing = GamificationProfile.newFor(studentId);
        when(gamificationRepository.findByStudentId(studentId)).thenReturn(Optional.of(existing));

        assertThat(service.profileOf(studentId)).isSameAs(existing);
    }

    @Test
    @DisplayName("atualizar preferencias aplica opt-in e telefone")
    void atualizaPreferencias() {
        Student student = student();
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Student updated = service.updatePreferences(studentId, false, "+5581988887777");

        assertThat(updated.notificationOptIn()).isFalse();
        assertThat(updated.phone()).isEqualTo("+5581988887777");
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("lista de alunos por turma vem do repositorio")
    void listaPorTurma() {
        UUID classroomId = UUID.randomUUID();
        when(studentRepository.findByClassroomId(classroomId)).thenReturn(List.of(student()));

        assertThat(service.byClassroom(classroomId)).hasSize(1);
    }
}
