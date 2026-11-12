package br.com.educamais.domain.enums;

/** Classificacao da resposta do aluno (RF17). */
public enum AnswerClassification {
    CORRECT,
    PARTIAL,
    INCORRECT;

    public boolean isSuccessful() {
        return this == CORRECT;
    }
}
