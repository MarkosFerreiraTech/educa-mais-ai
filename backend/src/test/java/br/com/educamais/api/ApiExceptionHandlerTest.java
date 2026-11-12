package br.com.educamais.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.api.exception.ApiError;
import br.com.educamais.api.exception.ApiExceptionHandler;
import br.com.educamais.domain.exception.AccessDeniedException;
import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.exception.DuplicateResourceException;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

/** RF36: contrato de erros da API (status HTTP x codigo estavel). */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/missions/1");

    @Test
    @DisplayName("recurso inexistente vira 404 com codigo estavel")
    void naoEncontrado() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Missao", "1"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/api/v1/missions/1");
    }

    @Test
    @DisplayName("duplicidade vira 409")
    void duplicado() {
        var response = handler.handleDuplicate(new DuplicateResourceException("EMAIL_IN_USE", "ja existe"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("EMAIL_IN_USE");
    }

    @Test
    @DisplayName("acesso negado de dominio vira 403")
    void accesoNegado() {
        var response = handler.handleAccessDenied(new AccessDeniedException("outro aluno"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID_CREDENTIALS,401",
            "INVALID_REFRESH_TOKEN,401",
            "SECURITY_HMAC_MISMATCH,401",
            "LOGIN_BLOCKED,429",
            "INSUFFICIENT_COINS,422",
            "ACCOUNT_NOT_ACTIVE,403",
            "MISSION_NOT_FINISHED,422"
    })
    @DisplayName("codigos de regra de negocio mapeiam para o status correto")
    void regrasDeNegocio(String code, int expectedStatus) {
        var response = handler.handleBusiness(new BusinessRuleException(code, "mensagem"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody().code()).isEqualTo(code);
    }

    @Test
    @DisplayName("erro inesperado nao vaza detalhe interno")
    void erroInesperado() {
        var response = handler.handleUnexpected(new IllegalStateException("stack interno"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).doesNotContain("stack interno");
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("corpo de erro sempre tem timestamp e path")
    void formatoPadrao() {
        ApiError error = ApiError.of(400, "VALIDATION_ERROR", "dados", "/api/v1/auth/register");
        assertThat(error.timestamp()).isNotNull();
        assertThat(error.violations()).isEmpty();
    }
}
