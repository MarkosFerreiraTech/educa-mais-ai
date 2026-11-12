package br.com.educamais.api.exception;

import br.com.educamais.domain.exception.AccessDeniedException;
import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.exception.DomainException;
import br.com.educamais.domain.exception.DuplicateResourceException;
import br.com.educamais.domain.exception.InvalidMissionTransitionException;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz excecoes de dominio em respostas HTTP consistentes (Problem-like). */
@RestControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(DomainException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DomainException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(DomainException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(InvalidMissionTransitionException.class)
    public ResponseEntity<ApiError> handleTransition(DomainException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessRuleException exception,
                                                   HttpServletRequest request) {
        HttpStatus status = switch (exception.code()) {
            case "INVALID_CREDENTIALS", "INVALID_REFRESH_TOKEN", "SECURITY_HMAC_MISMATCH",
                 "SECURITY_REPLAY_DETECTED", "SECURITY_INVALID_API_KEY", "SECURITY_INVALID_TIMESTAMP",
                 "SECURITY_MISSING_TIMESTAMP" -> HttpStatus.UNAUTHORIZED;
            case "LOGIN_BLOCKED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "INSUFFICIENT_COINS", "INSUFFICIENT_DIAMONDS", "INVALID_DIAMOND_AMOUNT",
                 "ITEM_ALREADY_OWNED" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "ACCOUNT_NOT_ACTIVE" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return build(status, exception, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        log.warn("VALIDATION_ERROR path={} erros={} detalhes={}", request.getRequestURI(),
                exception.getBindingResult().getErrorCount(),
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ":" + error.getDefaultMessage()).toList());
        List<ApiError.FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Dados invalidos na requisicao", violations, java.time.Instant.now(), request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Erros do proprio Spring MVC (JSON invalido, tipo incompativel, rota inexistente,
     * metodo nao suportado) preservam o status original em vez de virar 500.
     */
    @ExceptionHandler({
            org.springframework.web.ErrorResponseException.class,
            org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.HttpRequestMethodNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> handleSpringMvc(Exception exception, HttpServletRequest request) {
        HttpStatus status = exception instanceof org.springframework.web.ErrorResponse errorResponse
                ? HttpStatus.valueOf(errorResponse.getStatusCode().value())
                : HttpStatus.BAD_REQUEST;
        String code = switch (status) {
            case BAD_REQUEST -> "MALFORMED_REQUEST";
            case NOT_FOUND -> "ENDPOINT_NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "REQUEST_ERROR";
        };
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), code, "Requisicao invalida", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("UNEXPECTED_ERROR path={} type={}", request.getRequestURI(),
                exception.getClass().getSimpleName(), exception);
        ApiError error = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "Erro inesperado. Tente novamente.", request.getRequestURI());
        return ResponseEntity.internalServerError().body(error);
    }

    private ApiError.FieldViolation toViolation(FieldError fieldError) {
        return new ApiError.FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, DomainException exception,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), exception.code(),
                exception.getMessage(), request.getRequestURI()));
    }
}
