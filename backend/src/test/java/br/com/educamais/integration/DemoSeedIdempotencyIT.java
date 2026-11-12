package br.com.educamais.integration;

import static br.com.educamais.bootstrap.DemoDataSeeder.DEMO_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.educamais.bootstrap.DemoDataSeeder;
import br.com.educamais.crypto.AesGcmEncryptor;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.PasswordHasherPort;
import br.com.educamais.domain.port.StudentRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * O seeder do perfil {@code demo} roda em TODO startup. Se ele nao for idempotente, a API
 * entra em crash-loop — foi o que aconteceu no Docker:
 *
 * <pre>
 * ERROR: duplicate key value violates unique constraint "students_email_hash_key"
 * Detail: Key (email_hash)=(rkm9hKNcw8DNH6mSyVxkSTnuVFDwKohHDRIhRkbaO5E=) already exists.
 * </pre>
 *
 * <p>Causa: o {@link DemoDataSeeder} checava {@code findById(DEMO_STUDENT_ID)}, mas criava o
 * aluno com {@code Student.register(...)}, que gera UUID aleatorio. O id fixo nunca existia,
 * a checagem nunca acertava e o INSERT colidia no indice unico de {@code email_hash}.</p>
 *
 * <p>Este teste usa o H2 de teste com Flyway + JPA reais. Cada caso recria o contexto
 * ({@code @DirtiesContext}) para que a ordem de execucao nao influencie o resultado.</p>
 */
@ActiveProfiles({"demo", "test"})
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:educamais_seed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.flyway.locations=classpath:db/migration-h2",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("DemoDataSeeder: idempotencia real contra o banco")
class DemoSeedIdempotencyIT {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordHasherPort passwordHasher;

    @Autowired
    private AesGcmEncryptor encryptor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * O seed roda no startup do contexto (ApplicationRunner), entao cada teste ja recebe o
     * aluno demo criado. Para reproduzir o cenario "sem aluno demo" (alunoComOutroUuidNaoDuplica)
     * removemos antes as tabelas que referenciam students, na ordem inversa das FKs.
     */
    @BeforeEach
    void limparAlunoDemo() {
        String hash = encryptor.blindIndex(DEMO_EMAIL);
        String subquery = "SELECT id FROM students WHERE email_hash = ?";
        for (String tabela : java.util.List.of("diagnostic_attempts", "reward_ledger",
                "notifications", "teacher_reports", "avatar_items_owned",
                "gamification_profiles")) {
            jdbcTemplate.update("DELETE FROM " + tabela + " WHERE student_id IN (" + subquery + ")",
                    hash);
        }
        jdbcTemplate.update("DELETE FROM students WHERE email_hash = ?", hash);
    }

    @Test
    @DisplayName("rodar o seeder duas vezes nao viola students_email_hash_key")
    void rodarDuasVezesNaoQuebra() {
        assertThatCode(() -> {
            seeder.run(null);
            seeder.run(null);
        }).as("o segundo startup do container nao pode derrubar a API")
                .doesNotThrowAnyException();

        assertThat(emailHashCount(DemoDataSeeder.DEMO_EMAIL))
                .as("o e-mail demo existe uma unica vez em students")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("aluno demo pre-existente com OUTRO uuid nao gera INSERT duplicado")
    void alunoComOutroUuidNaoDuplica() {
        // Reproduz o banco de uma execucao anterior: mesmo e-mail, UUID aleatorio.
        Student antigo = studentRepository.save(Student.register("Ana Antiga",
                DemoDataSeeder.DEMO_EMAIL, "+5581988887777", LocalDate.of(2012, 4, 10),
                passwordHasher.hash("Antiga@2026"), DemoDataSeeder.DEMO_SCHOOL_ID,
                DemoDataSeeder.DEMO_CLASSROOM_ID, true));
        assertThat(antigo.id()).isNotEqualTo(DemoDataSeeder.DEMO_STUDENT_ID);

        assertThatCode(() -> seeder.run(null))
                .as("era aqui que o startup morria com duplicate key em students_email_hash_key")
                .doesNotThrowAnyException();

        assertThat(emailHashCount(DemoDataSeeder.DEMO_EMAIL))
                .as("nao criamos um segundo aluno com o mesmo e-mail")
                .isEqualTo(1);
        assertThat(studentRepository.findById(antigo.id()))
                .as("o aluno pre-existente e preservado")
                .isPresent();
    }

    @Test
    @DisplayName("o aluno demo criado pelo seeder e encontravel pelo UUID fixo")
    void alunoDemoTemIdFixo() {
        seeder.run(null);

        assertThat(studentRepository.findById(DemoDataSeeder.DEMO_STUDENT_ID))
                .as("DEMO_STUDENT_ID e usado pelos scripts e pela documentacao")
                .isPresent();
    }

    /**
     * Conta linhas por e-mail no banco usando o MESMO blind index que a aplicacao grava em
     * {@code students.email_hash} (a coluna e UNIQUE - e ela que estourava no startup).
     */
    private int emailHashCount(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM students WHERE email_hash = ?", Integer.class,
                encryptor.blindIndex(email));
        return count == null ? 0 : count;
    }
}
