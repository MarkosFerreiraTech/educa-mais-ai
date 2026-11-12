package br.com.educamais.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload de cadastro do aluno (RF01, RF03). */
public record RegisterRequest(
        @NotBlank(message = "nome e obrigatorio")
        @Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
        String name,

        @NotBlank(message = "e-mail e obrigatorio")
        @Email(message = "e-mail invalido")
        String email,

        @Size(max = 20, message = "telefone invalido")
        String phone,

        @Past(message = "data de nascimento deve ser no passado")
        LocalDate birthDate,

        @NotBlank(message = "senha e obrigatoria")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = ".*[0-9].*", message = "senha deve conter ao menos um numero")
        @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "senha deve conter ao menos um simbolo")
        String password,

        List<String> interests,

        UUID schoolId,

        UUID classroomId,

        Boolean whatsAppOptIn,

        @Pattern(regexp = "^(ACEITE|NAO_ACEITE)$", message = "aceite do responsavel invalido")
        String guardianConsent) {

    /** Opt-in de WhatsApp e opcional no payload: ausente significa "nao autorizado". */
    public boolean whatsAppOptInOrDefault() {
        return Boolean.TRUE.equals(whatsAppOptIn);
    }
}
