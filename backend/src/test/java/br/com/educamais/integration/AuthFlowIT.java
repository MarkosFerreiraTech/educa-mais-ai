package br.com.educamais.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** RF01-RF06, RNF06: cadastro, login, refresh, autorizacao por papel e perfil. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String register(String email) throws Exception {
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String uniqueEmail() {
        return "aluno-" + UUID.randomUUID() + "@escola.br";
    }

    @Test
    @DisplayName("cadastro devolve tokens e perfil acessivel com o Bearer")
    void cadastroLogado() throws Exception {
        String token = register(uniqueEmail());

        mockMvc.perform(get("/api/v1/students/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").isNotEmpty())
                .andExpect(jsonPath("$.interests").isArray());
    }

    @Test
    @DisplayName("login com senha correta devolve novo par de tokens")
    void loginValido() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Senha@2026\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("login com senha errada e 401 sem vazar detalhe")
    void loginInvalido() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"errada\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh troca o token de atualizacao por um access token")
    void refreshToken() throws Exception {
        String email = uniqueEmail();
        String access = register(email);
        assertThat(access).isNotBlank();

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Senha@2026\"}".formatted(email)))
                .andReturn();
        String refreshToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("endpoint protegido sem token e barrado pela autenticacao")
    void semToken() throws Exception {
        mockMvc.perform(get("/api/v1/missions"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isIn(401, 403));
    }

    @Test
    @DisplayName("aluno nao acessa o dashboard do professor (403)")
    void rbacNegado() throws Exception {
        String token = register(uniqueEmail());
        mockMvc.perform(get("/api/v1/teacher/dashboard").param("classroomId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("atualiza interesses do aluno autenticado")
    void atualizaInteresses() throws Exception {
        String token = register(uniqueEmail());

        mockMvc.perform(patch("/api/v1/auth/interests").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("[\"musica\",\"ciencia\"]"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interests").value(org.hamcrest.Matchers.hasItem("musica")));
    }

    @Test
    @DisplayName("cadastro com e-mail duplicado e rejeitado (409)")
    void emailDuplicado() throws Exception {
        String email = uniqueEmail();
        register(email);

        String body = """
                {"name":"Outra Pessoa","email":"%s","phone":"+5581977776666","birthDate":"2011-02-02",
                 "password":"Senha@2026","interests":["games"],"whatsAppOptIn":false}
                """.formatted(email);
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("payload invalido responde 400 com o campo culpado")
    void validacaoDeEntrada() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalido\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
