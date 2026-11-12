package br.com.educamais.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.domain.enums.QuestionType;
import br.com.educamais.domain.model.DiagnosticQuestion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF16, RF17. */
class DiagnosticQuestionTest {

    private DiagnosticQuestion question() {
        return new DiagnosticQuestion(null, UUID.randomUUID(), UUID.randomUUID(),
                "Qual fracao equivale a 2/4?", QuestionType.MULTIPLE_CHOICE, List.of("1/2", "2/3", "3/4", "1/3"),
                "1/2", "2/4 simplifica para 1/2", 2);
    }

    @Test
    @DisplayName("Resposta correta e reconhecida ignorando caixa e espacos")
    void respostaCorreta() {
        assertThat(question().matches(" 1/2 ")).isTrue();
        assertThat(question().matches("1/3")).isFalse();
        assertThat(question().matches(null)).isFalse();
    }

    @Test
    @DisplayName("Dificuldade e limitada entre 1 e 5")
    void dificuldadeLimitada() {
        assertThat(question().withDifficulty(9).difficulty()).isEqualTo(5);
        assertThat(question().withDifficulty(0).difficulty()).isEqualTo(1);
    }
}
