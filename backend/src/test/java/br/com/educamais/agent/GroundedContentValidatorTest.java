package br.com.educamais.agent;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.agent.validation.GroundedContentValidator;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Syllabus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF15: ancoragem obrigatoria na ementa. */
class GroundedContentValidatorTest {

    private final GroundedContentValidator validator = new GroundedContentValidator();
    private final Syllabus syllabus = new Syllabus(
            List.of("Comparar fracoes com denominadores diferentes"),
            List.of("fracoes equivalentes", "minimo multiplo comum"), List.of(), "BNCC");

    @Test
    @DisplayName("Passo ancorado na ementa e mantido")
    void passoAncorado() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Comparar fracoes equivalentes", "use fracoes", "2/4 = 1/2"),
                new MissionPlan.PlannedStep("Encontrar o minimo multiplo comum", "use mmc", "mmc(2,4)=4")),
                PlanSource.LLM);

        GroundedContentValidator.ValidationOutcome outcome = validator.validate(plan, syllabus);

        assertThat(outcome.hasValidPlan()).isTrue();
        assertThat(outcome.discardedObjectives()).isEmpty();
    }

    @Test
    @DisplayName("Passo fora da ementa e descartado")
    void passoDesancorado() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Estudar numeros complexos", "numeros imaginarios", "i ao quadrado = -1"),
                new MissionPlan.PlannedStep("Comparar fracoes equivalentes", "fracoes", "2/4 = 1/2")),
                PlanSource.LLM);

        GroundedContentValidator.ValidationOutcome outcome = validator.validate(plan, syllabus);

        assertThat(outcome.discardedObjectives()).hasSize(1);
        assertThat(outcome.discardedObjectives().get(0)).contains("numeros complexos");
        assertThat(outcome.plan().steps()).hasSize(1);
        assertThat(outcome.hasValidPlan()).isFalse();
        assertThat(outcome.plan().steps().get(0).objective()).contains("Comparar fracoes");
    }

    @Test
    @DisplayName("Ementa sem objetivos nao descarta nada")
    void ementaVazia() {
        MissionPlan plan = new MissionPlan(List.of(
                new MissionPlan.PlannedStep("Objetivo qualquer", "explicacao", "exemplo"),
                new MissionPlan.PlannedStep("Outro objetivo", "explicacao", "exemplo")), PlanSource.LLM);

        GroundedContentValidator.ValidationOutcome outcome = validator.validate(plan, new Syllabus(null, null,
                null, null));

        assertThat(outcome.plan().steps()).hasSize(2);
    }

    @Test
    @DisplayName("Plano nulo retorna resultado invalido")
    void planoNulo() {
        assertThat(validator.validate(null, syllabus).hasValidPlan()).isFalse();
    }
}
