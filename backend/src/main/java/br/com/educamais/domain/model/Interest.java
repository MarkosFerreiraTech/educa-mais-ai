package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.InterestCode;

/** Interesse pessoal do aluno, com prioridade de uso nos exemplos (RF03). */
public record Interest(InterestCode code, int priority) {

    public Interest {
        if (code == null) {
            throw new IllegalArgumentException("Interesse e obrigatorio");
        }
        if (priority < 1) {
            priority = 1;
        }
    }

    public static Interest of(InterestCode code, int priority) {
        return new Interest(code, priority);
    }
}
