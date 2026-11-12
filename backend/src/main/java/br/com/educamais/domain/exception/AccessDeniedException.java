package br.com.educamais.domain.exception;

public class AccessDeniedException extends DomainException {

    public AccessDeniedException(String message) {
        super("ACCESS_DENIED", message);
    }
}
