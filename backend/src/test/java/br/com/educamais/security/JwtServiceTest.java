package br.com.educamais.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF02, RNF06: emissao e validacao de JWT. */
class JwtServiceTest {

    private JwtService jwtService;
    private final UUID studentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("chave-de-teste-com-pelo-menos-32-caracteres!");
        properties.setIssuer("educa-mais-test");
        properties.setAudience("educa-mais-test-web");
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("Access token carrega subject, papel e escola")
    void accessToken() {
        String token = jwtService.generateAccessToken(studentId, "ana@escola.br", Role.ALUNO, schoolId);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(studentId.toString());
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("ALUNO");
        assertThat(claims.get(JwtService.CLAIM_SCHOOL, String.class)).isEqualTo(schoolId.toString());
        assertThat(claims.get(JwtService.CLAIM_TYPE, String.class)).isEqualTo("access");
    }

    @Test
    @DisplayName("Principal reflete as claims do token")
    void principal() {
        String token = jwtService.generateAccessToken(studentId, "ana@escola.br", Role.PROFESSOR, schoolId);
        AuthenticatedPrincipal principal = jwtService.toPrincipal(jwtService.parseClaims(token));

        assertThat(principal.subjectId()).isEqualTo(studentId);
        assertThat(principal.role()).isEqualTo(Role.PROFESSOR);
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_PROFESSOR");
    }

    @Test
    @DisplayName("Token adulterado e rejeitado")
    void tokenAdulterado() {
        String token = jwtService.generateAccessToken(studentId, "ana@escola.br", Role.ALUNO, schoolId);
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwtService.parseClaims(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Refresh token tem tipo refresh e nao serve como access")
    void refreshToken() {
        String refresh = jwtService.generateRefreshToken(studentId);
        assertThat(jwtService.parseClaims(refresh).get(JwtService.CLAIM_TYPE, String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Segredo curto impede a inicializacao")
    void segredoCurto() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("curto");
        assertThatThrownBy(() -> new JwtService(properties)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Tempo de vida do access token e de 60 minutos")
    void ttl() {
        assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(3600);
    }
}
