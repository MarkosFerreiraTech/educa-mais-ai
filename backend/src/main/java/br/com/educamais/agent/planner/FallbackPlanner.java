package br.com.educamais.agent.planner;

import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Syllabus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Plano deterministico usado quando o LLM falha ou devolve conteudo invalido (RF21).
 * Garante que o aluno nunca fique sem missao.
 */
@Component
public class FallbackPlanner {

    private static final int HARD_LIMIT = MissionPlan.MAX_STEPS;

    public MissionPlan plan(RecoveryPlanRequest request) {
        Syllabus syllabus = request.lesson().syllabus();
        List<String> objectives = syllabus.hasObjectives()
                ? syllabus.learningObjectives()
                : List.of("Compreender o conteudo da aula: " + request.lesson().topic());
        String hint = request.interestHint();
        String topic = request.lesson().topic();

        List<MissionPlan.PlannedStep> steps = new ArrayList<>();
        int index = 1;
        for (String objective : objectives) {
            if (steps.size() >= HARD_LIMIT) {
                break;
            }
            steps.add(new MissionPlan.PlannedStep(
                    objective,
                    "Objetivo %d: %s. Vamos entender a ideia central de %s comparando partes de um todo "
                            .concat("com situacoes de %s.").formatted(index, objective, topic, hint),
                    "Exemplo com %s: divida uma situacao em partes iguais e conte quantas voce usou.".formatted(hint)));
            index++;
        }
        while (steps.size() < MissionPlan.MIN_STEPS) {
            steps.add(new MissionPlan.PlannedStep(
                    "Praticar " + topic,
                    "Agora vamos praticar %s com um exemplo passo a passo usando %s.".formatted(topic, hint),
                    "Exemplo guiado: identifique as partes, conte as partes usadas e escreva o resultado."));
        }
        return new MissionPlan(steps, PlanSource.FALLBACK);
    }
}
