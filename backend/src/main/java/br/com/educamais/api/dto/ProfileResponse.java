package br.com.educamais.api.dto;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id, String name, String email, String phone, String status,
                              List<String> interests, boolean whatsAppOptIn) {
}
