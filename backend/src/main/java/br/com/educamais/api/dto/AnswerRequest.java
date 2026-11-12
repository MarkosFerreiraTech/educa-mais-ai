package br.com.educamais.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AnswerRequest(@NotNull UUID questionId, @NotBlank String answer) {
}
