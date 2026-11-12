package br.com.educamais.bootstrap;

import br.com.educamais.api.dto.RegisterRequest;
import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.Syllabus;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.ShopRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados de demonstracao (perfil {@code demo}): turma, aluno campeao do fluxo, ementa de fracoes
 * e itens da loja. Permite rodar o ciclo autonomo completo sem cadastro manual.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final UUID DEMO_SCHOOL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID DEMO_CLASSROOM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID DEMO_LESSON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEMO_STUDENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final ShopRepository shopRepository;
    private final br.com.educamais.service.AuthService authService;

    public DemoDataSeeder(StudentRepository studentRepository, LessonRepository lessonRepository,
                          ShopRepository shopRepository, br.com.educamais.service.AuthService authService) {
        this.studentRepository = studentRepository;
        this.lessonRepository = lessonRepository;
        this.shopRepository = shopRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedStudent();
        seedLesson();
        seedShop();
        log.info("DEMO_DATA_READY studentId={} lessonId={}", DEMO_STUDENT_ID, DEMO_LESSON_ID);
    }

    private void seedStudent() {
        if (studentRepository.findById(DEMO_STUDENT_ID).isPresent()) {
            return;
        }
        Student student = Student.register("Ana Souza", "ana.souza@escola.br", "+5581999998888",
                LocalDate.of(2012, 4, 10), "{noop}demo", DEMO_SCHOOL_ID, DEMO_CLASSROOM_ID, true);
        student.defineInterests(List.of(Interest.of(InterestCode.FOOTBALL, 5),
                Interest.of(InterestCode.GAMES, 3)));
        studentRepository.save(student);
        log.info("DEMO_STUDENT_CREATED email=ana.souza@escola.br password=Demo@2026");
    }

    private void seedLesson() {
        if (lessonRepository.findById(DEMO_LESSON_ID).isPresent()) {
            return;
        }
        Syllabus syllabus = new Syllabus(
                List.of("Comparar fracoes com denominadores diferentes",
                        "Representar fracoes na reta numerica"),
                List.of("fracoes equivalentes", "minimo multiplo comum", "comparacao de fracoes"),
                List.of("fracoes com mesmo denominador"),
                "BNCC EF06MA07 - Curriculo de Matematica, 6o ano");
        lessonRepository.save(new Lesson(DEMO_LESSON_ID, DEMO_CLASSROOM_ID, "Matematica",
                "Fracoes equivalentes", LocalDate.now().minusDays(1), syllabus));
        log.info("DEMO_LESSON_CREATED topic=Fracoes equivalentes");
    }

    private void seedShop() {
        if (!shopRepository.findAll().isEmpty()) {
            return;
        }
        shopRepository.save(new ShopItem(null, "camisa-time", "Camisa do Time", "OUTFIT", 20, "/assets/avatar/shirt.png"));
        shopRepository.save(new ShopItem(null, "chuteira-dourada", "Chuteira Dourada", "ACCESSORY", 30,
                "/assets/avatar/boots.png"));
        shopRepository.save(new ShopItem(null, "bone-aba-reta", "Bone Aba Reta", "ACCESSORY", 15,
                "/assets/avatar/cap.png"));
        shopRepository.save(new ShopItem(null, "fone-gamer", "Fone Gamer", "ACCESSORY", 25, "/assets/avatar/headset.png"));
        log.info("DEMO_SHOP_CREATED items=4");
    }

    /** Helper usado pelos testes de integracao do fluxo demo. */
    public void registerDemoStudent(RegisterRequest request) {
        authService.register(request.name(), request.email(), request.phone(), request.birthDate(),
                request.password(), List.of(Interest.of(InterestCode.FOOTBALL, 5)), DEMO_SCHOOL_ID,
                DEMO_CLASSROOM_ID, true);
    }
}
