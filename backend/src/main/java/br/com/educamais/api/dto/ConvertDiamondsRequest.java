package br.com.educamais.api.dto;

import jakarta.validation.constraints.Min;

public record ConvertDiamondsRequest(@Min(1) int amount) {
}
