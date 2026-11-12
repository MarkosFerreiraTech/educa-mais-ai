package br.com.educamais.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.domain.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF03/RNF02: principal JWT stateless exposto ao Spring Security. */
class AuthenticatedPrincipalTest {

    @Test
    @DisplayName("papel vira authority ROLE_*")
    void authorityPorPapel() {
        AuthenticatedPrincipal aluno = principal(Role.ALUNO);
        AuthenticatedPrincipal professor = principal(Role.PROFESSOR);

        assertThat(aluno.getAuthorities()).extracting("authority").containsExactly("ROLE_ALUNO");
        assertThat(professor.getAuthorities()).extracting("authority").containsExactly("ROLE_PROFESSOR");
    }

    @Test
    @DisplayName("conta nunca expira, nunca bloqueia e nao guarda senha")
    void contaSempreAtivaSemSenha() {
        AuthenticatedPrincipal principal = principal(Role.ADMIN);

        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    @DisplayName("username e o e-mail autenticado")
    void usernameEhEmail() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(UUID.randomUUID(),
                "ana@escola.br", Role.ALUNO, UUID.randomUUID());
        assertThat(principal.getUsername()).isEqualTo("ana@escola.br");
    }

    private static AuthenticatedPrincipal principal(Role role) {
        return new AuthenticatedPrincipal(UUID.randomUUID(), "user@escola.br", role, UUID.randomUUID());
    }
}
