package br.com.educamais.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.Syllabus;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.security.HmacSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Fluxo autonomo de ponta a ponta (RF01-RF33):
 * cadastro -> webhook de falta -> missao planejada -> notificacao -> recompensa -> relatorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutonomousCycleIT {

    private static final String API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private HmacSignatureValidator signatureValidator;

    /* ----------------------------- helpers ----------------------------- */

    private String registerStudent() throws Exception {
        String email = "aluno-" + UUID.randomUUID() + "@escola.br";
        String body = """
                {"name":"Ana Souza","email":"%s","phone":"+5581999998888","birthDate":"2012-04-10",
                 "password":"Senha@2026","interests":["futebol","games"],"whatsAppOptIn":true}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ALUNO"))
                .andReturn();
        return json(result).path("accessToken").asText();
    }

    private String studentIdOf(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/students/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").isNotEmpty())
                .andExpect(jsonPath("$.interests").isArray())
                .andReturn();
        return json(result).path("id").asText();
    }

    private UUID createLesson() {
        Syllabus syllabus = new Syllabus(
                List.of("Comparar fracoes com denominadores diferentes",
                        "Representar fracoes na reta numerica"),
                List.of("fracoes equivalentes", "minimo multiplo comum"),
                List.of("fracoes com mesmo denominador"), "BNCC EF06MA07");
        Lesson lesson = lessonRepository.save(new Lesson(null, UUID.randomUUID(), "Matematica",
                "Fracoes equivalentes", LocalDate.now(), syllabus));
        return lesson.id();
    }

    /** Requisicao de webhook com HMAC valido sobre o payload canonico. */
    private MockHttpServletRequestBuilder signedAttendance(String eventId, String studentId, UUID lessonId) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String canonical = eventId + "|" + studentId + "|" + lessonId;
        String signature = signatureValidator.hmac(timestamp + "." + canonical);
        String body = """
                {"eventId":"%s","studentId":"%s","lessonId":"%s","occurredAt":"%s","reason":"Falta registrada"}
                """.formatted(eventId, studentId, lessonId, Instant.now());
        return post("/api/v1/integrations/school/attendance")
                .header(HmacSignatureValidator.HEADER_API_KEY, API_KEY)
                .header(HmacSignatureValidator.HEADER_SIGNATURE, signature)
                .header(HmacSignatureValidator.HEADER_TIMESTAMP, timestamp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** O agente roda de forma assincrona; aguarda a missao aparecer. */
    private JsonNode awaitMissions(String token, int minimum) throws Exception {
        return await(() -> {
            try {
                return json(mockMvc.perform(get("/api/v1/missions").header("Authorization", "Bearer " + token))
                        .andReturn());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }, node -> node.size() >= minimum);
    }

    private JsonNode await(Supplier<JsonNode> supplier, java.util.function.Predicate<JsonNode> condition)
            throws Exception {
        JsonNode node = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            node = supplier.get();
            if (condition.test(node)) {
                return node;
            }
            Thread.sleep(150);
        }
        return node;
    }

    /* ------------------------------- testes ------------------------------ */

    @Test
    @DisplayName("Ciclo autonomo: falta gera missao planejada e notificacoes")
    void cicloCompletoCriaMissaoENotificacao() throws Exception {
        String token = registerStudent();
        String studentId = studentIdOf(token);
        UUID lessonId = createLesson();
        String eventId = "evt-" + UUID.randomUUID();

        mockMvc.perform(signedAttendance(eventId, studentId, lessonId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value(eventId));

        JsonNode missions = awaitMissions(token, 1);
        assertThat(missions.size()).isEqualTo(1);

        JsonNode mission = missions.get(0);
        assertThat(mission.path("planSource").asText()).isEqualTo("FALLBACK");
        assertThat(mission.path("steps").size()).isGreaterThanOrEqualTo(2);
        assertThat(mission.path("status").asText()).isIn("NOTIFIED", "IN_PROGRESS");
        assertThat(mission.path("steps").get(0).path("explanation").asText()).containsIgnoringCase("futebol");

        JsonNode notifications = json(mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + token)).andReturn());
        assertThat(notifications.size()).isGreaterThanOrEqualTo(2);
        assertThat(notifications.toString()).contains("IN_APP");
        assertThat(notifications.toString()).contains("SKIPPED");
    }

    @Test
    @DisplayName("Evento duplicado retorna 200 IGNORED_DUPLICATE (RF09)")
    void eventoDuplicadoEIdempotente() throws Exception {
        String token = registerStudent();
        String studentId = studentIdOf(token);
        UUID lessonId = createLesson();
        String eventId = "evt-dup-" + UUID.randomUUID();

        mockMvc.perform(signedAttendance(eventId, studentId, lessonId)).andExpect(status().isAccepted());
        mockMvc.perform(signedAttendance(eventId, studentId, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED_DUPLICATE"));

        assertThat(awaitMissions(token, 1).size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Webhook com assinatura invalida retorna 401 (RF07)")
    void assinaturaInvalida() throws Exception {
        String token = registerStudent();
        String studentId = studentIdOf(token);
        UUID lessonId = createLesson();
        String body = """
                {"eventId":"evt-x","studentId":"%s","lessonId":"%s"}
                """.formatted(studentId, lessonId);

        mockMvc.perform(post("/api/v1/integrations/school/attendance")
                        .header(HmacSignatureValidator.HEADER_API_KEY, API_KEY)
                        .header(HmacSignatureValidator.HEADER_SIGNATURE, "assinatura-invalida")
                        .header(HmacSignatureValidator.HEADER_TIMESTAMP,
                                String.valueOf(Instant.now().getEpochSecond()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SECURITY_HMAC_MISMATCH"));
    }

    @Test
    @DisplayName("Endpoints protegidos exigem JWT valido (RNF06)")
    void exigeJwt() throws Exception {
        mockMvc.perform(get("/api/v1/missions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/students/me").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Aluno acessa missao de outro aluno e recebe 403 (RF28)")
    void isolamentoEntreAlunos() throws Exception {
        String tokenA = registerStudent();
        String tokenB = registerStudent();
        String studentIdA = studentIdOf(tokenA);
        UUID lessonId = createLesson();

        mockMvc.perform(signedAttendance("evt-iso-" + UUID.randomUUID(), studentIdA, lessonId))
                .andExpect(status().isAccepted());

        JsonNode missions = awaitMissions(tokenA, 1);
        String missionId = missions.get(0).path("id").asText();

        mockMvc.perform(get("/api/v1/missions/" + missionId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Cadastro rejeita senha fraca com VALIDATION_ERROR")
    void validacoesDeCadastro() throws Exception {
        String body = """
                {"name":"Ana","email":"ana@escola.br","password":"fraca","interests":["futebol"]}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Login com credenciais invalidas retorna 401")
    void loginInvalido() throws Exception {
        String body = """
                {"email":"ninguem@escola.br","password":"Senha@2026"}
                """;
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Gamificacao inicia zerada para aluno novo")
    void gamificacaoInicial() throws Exception {
        String token = registerStudent();
        mockMvc.perform(get("/api/v1/students/me/gamification").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.diamonds").value(0));
    }

    @Test
    @DisplayName("Loja exige autenticacao")
    void lojaProtegida() throws Exception {
        mockMvc.perform(get("/api/v1/shop/items")).andExpect(status().isUnauthorized());
        String token = registerStudent();
        mockMvc.perform(get("/api/v1/shop/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Health check exposto sem autenticacao (RF40)")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
