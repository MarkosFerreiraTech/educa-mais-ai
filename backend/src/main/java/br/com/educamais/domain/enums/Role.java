package br.com.educamais.domain.enums;

/** Papeis de acesso (RBAC). */
public enum Role {
    ALUNO,
    PROFESSOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
