package br.com.educamais.domain.exception;

public class InvalidMissionTransitionException extends DomainException {

    public InvalidMissionTransitionException(String message) {
        super("INVALID_MISSION_TRANSITION", message);
    }
}
