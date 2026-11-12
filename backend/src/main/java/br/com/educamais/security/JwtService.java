package br.com.educamais.security;

import br.com.educamais.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Emissao e validacao de JWT HS256 (RF02 / RNF06).
 * Valida assinatura, emissor, audiencia e expiracao.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROLE = "roles";
    public static final String CLAIM_SCHOOL = "schoolId";
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter ao menos 32 caracteres");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID subjectId, String email, Role role, UUID schoolId) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(properties.getAccessTokenMinutes() * 60);
        return Jwts.builder()
                .subject(subjectId.toString())
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_SCHOOL, schoolId == null ? null : schoolId.toString())
                .claim(CLAIM_TYPE, "access")
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID subjectId) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(properties.getRefreshTokenDays() * 24 * 3600);
        return Jwts.builder()
                .subject(subjectId.toString())
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .claim(CLAIM_TYPE, "refresh")
                .signWith(signingKey)
                .compact();
    }

    /** Lanca {@link JwtException} para token invalido/expirado (mapeado para 401). */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthenticatedPrincipal toPrincipal(Claims claims) {
        String schoolId = claims.get(CLAIM_SCHOOL, String.class);
        return new AuthenticatedPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                schoolId == null ? null : UUID.fromString(schoolId));
    }

    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenMinutes() * 60;
    }

    public Map<String, Object> publicJwk() {
        return Map.of(
                "kty", "oct",
                "use", "sig",
                "alg", "HS256",
                "kid", "educa-mais-" + properties.getIssuer());
    }
}
