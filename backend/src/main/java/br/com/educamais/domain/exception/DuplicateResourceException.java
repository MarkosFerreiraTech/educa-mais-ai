package br.com.educamais.domain.exception;

public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String code, String message) {
        super(code, message);
    }
}
