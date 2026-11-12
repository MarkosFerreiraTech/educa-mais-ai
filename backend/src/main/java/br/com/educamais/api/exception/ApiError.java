package br.com.educamais.api.exception;

import java.time.Instant;
import java.util.List;

/** Corpo padrao de erro da API. */
public record ApiError(int status, String code, String message, List<FieldViolation> violations,
                       Instant timestamp, String path) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(status, code, message, List.of(), Instant.now(), path);
    }

    /** Violacao de campo (Bean Validation). */
    public record FieldViolation(String field, String message) {
    }
}
