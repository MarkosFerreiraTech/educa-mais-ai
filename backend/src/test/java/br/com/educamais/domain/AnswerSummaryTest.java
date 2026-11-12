package br.com.educamais.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.domain.enums.AnswerClassification;
import br.com.educamais.domain.model.AnswerSummary;
import br.com.educamais.domain.model.DiagnosticAttempt;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF34: consolidado de desempenho enviado ao professor. */
class AnswerSummaryTest {

    private static DiagnosticAttempt attempt(AnswerClassification classification) {
        return DiagnosticAttempt.of(UUID.randomUUID(), UUID.randomUUID(), "2/4", classification, "feedback");
    }

    @Test
    @DisplayName("conta acertos, parciais e erros")
    void contaPorClassificacao() {
        AnswerSummary summary = AnswerSummary.of(List.of(
                attempt(AnswerClassification.CORRECT),
                attempt(AnswerClassification.CORRECT),
                attempt(AnswerClassification.PARTIAL),
                attempt(AnswerClassification.INCORRECT)), List.of("fracoes equivalentes"));

        assertThat(summary.correct()).isEqualTo(2);
        assertThat(summary.partial()).isEqualTo(1);
        assertThat(summary.incorrect()).isEqualTo(1);
        assertThat(summary.total()).isEqualTo(4);
        assertThat(summary.successRate()).isEqualTo(50);
        assertThat(summary.gaps()).containsExactly("fracoes equivalentes");
    }

    @Test
    @DisplayName("sem tentativas a taxa e 100 e nao ha lacunas")
    void vazio() {
        AnswerSummary summary = AnswerSummary.empty();
        assertThat(summary.total()).isZero();
        assertThat(summary.successRate()).isEqualTo(100);
        assertThat(summary.gaps()).isEmpty();
    }

    @Test
    @DisplayName("lacunas nulas viram lista vazia (imutavel)")
    void lacunasNulas() {
        AnswerSummary summary = new AnswerSummary(1, 0, 0, null);
        assertThat(summary.gaps()).isEmpty();
    }

    @Test
    @DisplayName("taxa de sucesso arredonda para o inteiro mais proximo")
    void arredondamento() {
        AnswerSummary summary = AnswerSummary.of(List.of(
                attempt(AnswerClassification.CORRECT),
                attempt(AnswerClassification.INCORRECT),
                attempt(AnswerClassification.INCORRECT)), List.of());
        assertThat(summary.successRate()).isEqualTo(33);
    }
}
