package br.com.educamais.api.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                            String role) {
}
