package br.com.educamais.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.PasswordHasherPort;
import br.com.educamais.domain.port.ShopRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Regressao do aluno de demonstracao.
 *
 * <p>O seeder gravava o hash {@code "{noop}demo"}, que o {@code BCryptPasswordEncoder} nunca
 * valida — a credencial {@code ana.souza@escola.br / Demo@2026} anunciada no README e usada nos
 * scripts simplesmente nao funcionava (login sempre 401).</p>
 *
 * <p>Aqui o seeder roda com o {@link BCryptPasswordEncoder} real e o teste confirma que a senha
 * documentada realmente valida contra o hash persistido.</p>
 */
@DisplayName("DemoDataSeeder: credencial de demonstracao valida de verdade")
class DemoDataSeederTest {

    private StudentRepository studentRepository;
    private LessonRepository lessonRepository;
    private ShopRepository shopRepository;
    private final PasswordHasherPort passwordHasher = new BCryptHasherForTest();

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        lessonRepository = mock(LessonRepository.class);
        shopRepository = mock(ShopRepository.class);
        when(studentRepository.findById(any())).thenReturn(Optional.empty());
        when(lessonRepository.findById(any())).thenReturn(Optional.empty());
        when(shopRepository.findAll()).thenReturn(List.of());
    }

    @Test
    @DisplayName("a senha Demo@2026 valida contra o hash gravado (antes era {noop}, nunca validava)")
    void senhaDeDemoValida() {
        new DemoDataSeeder(studentRepository, lessonRepository, shopRepository, passwordHasher).run(null);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student seeded = captor.getValue();

        assertThat(seeded.passwordHash())
                .as("hash precisa ser BCrypt, nao {noop}")
                .startsWith("$2")
                .doesNotContain("{noop}");
        assertThat(passwordHasher.matches(DemoDataSeeder.DEMO_PASSWORD, seeded.passwordHash()))
                .as("a credencial documentada no README precisa funcionar")
                .isTrue();
        assertThat(passwordHasher.matches("senha-errada", seeded.passwordHash())).isFalse();
    }

    @Test
    @DisplayName("e idempotente: aluno ja existente nao e recriado")
    void naoRecriaAlunoExistente() {
        when(studentRepository.findById(DemoDataSeeder.DEMO_STUDENT_ID))
                .thenReturn(Optional.of(Student.register("Existente", "existente@escola.br", null, null,
                        passwordHasher.hash("qualquer"), DemoDataSeeder.DEMO_SCHOOL_ID,
                        DemoDataSeeder.DEMO_CLASSROOM_ID, false)));

        new DemoDataSeeder(studentRepository, lessonRepository, shopRepository, passwordHasher).run(null);

        verify(studentRepository, never()).save(any());
    }

    /** Adapter fino sobre o BCrypt real, com o mesmo custo do bean de producao. */
    private static final class BCryptHasherForTest implements PasswordHasherPort {

        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        @Override
        public String hash(String rawPassword) {
            return encoder.encode(rawPassword);
        }

        @Override
        public boolean matches(String rawPassword, String hashedPassword) {
            return hashedPassword != null && encoder.matches(rawPassword, hashedPassword);
        }
    }
}
