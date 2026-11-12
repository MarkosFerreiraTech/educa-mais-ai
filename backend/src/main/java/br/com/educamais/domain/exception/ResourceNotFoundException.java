package br.com.educamais.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", "%s nao encontrado: %s".formatted(resource, id));
    }
}
