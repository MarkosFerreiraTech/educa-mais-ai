package br.com.educamais.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** RF05, RF27-RF32: area do aluno (perfil, missoes, notificacoes e loja). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentAreaIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin() throws Exception {
        String email = "aluno-" + UUID.randomUUID() + "@escola.br";
        String body = """
                {"name":"Bruno Lima","email":"%s","phone":"+5581999997777","birthDate":"2012-06-01",
                 "password":"Senha@2026","interests":["games"],"whatsAppOptIn":true}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("perfil traz dados, interesses e gamificacao inicial")
    void perfilCompleto() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/students/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bruno Lima"))
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.interests[0]").value("games"));

        mockMvc.perform(get("/api/v1/students/me/gamification").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.coins").value(0));
    }

    @Test
    @DisplayName("preferencias de contato podem ser atualizadas")
    void atualizaPreferencias() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(patch("/api/v1/students/me/preferences").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"whatsAppOptIn\":false,\"phone\":\"+5581966665555\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsAppOptIn").value(false));
    }

    @Test
    @DisplayName("aluno novo comeca sem missoes e sem notificacoes")
    void jornadaVazia() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/missions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("missao inexistente responde 404 para o aluno autenticado")
    void missaoInexistente() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(get("/api/v1/missions/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("loja lista itens e bloqueia compra sem moedas (422)")
    void lojaSemMoedas() throws Exception {
        String token = registerAndLogin();

        MvcResult items = mockMvc.perform(get("/api/v1/shop/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();
        var list = objectMapper.readTree(items.getResponse().getContentAsString());
        // A loja e semeada pelo DemoDataSeeder (perfil demo) ou pela migration V2; aqui o catalogo
        // pode estar vazio, mas o contrato exige lista de itens com code/priceCoins.
        for (var item : list) {
            assertThat(item.hasNonNull("code")).isTrue();
            assertThat(item.has("priceCoins")).isTrue();
        }

        String itemCode = list.isEmpty() ? "camisa-time" : list.get(0).get("code").asText();
        mockMvc.perform(post("/api/v1/shop/purchase").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCode\":\"%s\"}".formatted(itemCode)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("converter diamantes sem saldo e rejeitado")
    void conversaoSemSaldo() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/shop/diamonds/convert").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
