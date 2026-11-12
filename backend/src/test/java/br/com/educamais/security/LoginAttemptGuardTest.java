package br.com.educamais.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RNF14 / UC09-E5: bloqueio apos 10 falhas de login em 5 minutos. */
class LoginAttemptGuardTest {

    @Test
    @DisplayName("identificador desconhecido nunca esta bloqueado")
    void semHistorico() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        assertThatCode(() -> guard.checkAllowed("ana@escola.br")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloqueia na decima falha consecutiva")
    void bloqueiaAposDezFalhas() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        for (int attempt = 0; attempt < 9; attempt++) {
            guard.registerFailure("ana@escola.br");
            guard.checkAllowed("ana@escola.br");
        }
        guard.registerFailure("ana@escola.br");

        assertThatThrownBy(() -> guard.checkAllowed("ana@escola.br"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Muitas tentativas");
    }

    @Test
    @DisplayName("sucesso zera o contador")
    void sucessoLimpaContador() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        for (int attempt = 0; attempt < 10; attempt++) {
            guard.registerFailure("professor@escola.br");
        }
        guard.registerSuccess("professor@escola.br");

        assertThatCode(() -> guard.checkAllowed("professor@escola.br")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("identificador nulo cai na chave anonima sem quebrar")
    void identificadorNulo() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        guard.registerFailure(null);
        assertThatCode(() -> guard.checkAllowed("outro@escola.br")).doesNotThrowAnyException();
        guard.registerSuccess(null);
    }
}
