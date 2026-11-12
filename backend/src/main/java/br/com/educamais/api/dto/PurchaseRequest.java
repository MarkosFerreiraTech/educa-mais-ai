package br.com.educamais.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(@NotBlank String itemCode) {
}
