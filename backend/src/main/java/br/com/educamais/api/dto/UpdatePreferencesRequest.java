package br.com.educamais.api.dto;

public record UpdatePreferencesRequest(boolean whatsAppOptIn, String phone) {
}
