package br.com.educamais.domain.exception;

/** Base das excecoes de negocio (mapeadas para HTTP no ApiExceptionHandler). */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
