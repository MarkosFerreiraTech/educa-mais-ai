package br.com.educamais.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Claims e tempos de vida do JWT (RF02). */
@ConfigurationProperties(prefix = "educamais.security.jwt")
public class JwtProperties {

    private String secret;
    private String issuer = "educa-mais-api";
    private String audience = "educa-mais-web";
    private long accessTokenMinutes = 60;
    private long refreshTokenDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }
}
