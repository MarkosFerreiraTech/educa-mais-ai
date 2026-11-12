package br.com.educamais.agent.planner;

import br.com.educamais.agent.validation.GroundedContentValidator;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.port.LlmPort;
import br.com.educamais.domain.port.LlmPort.PlanPrompt;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Planejamento pedagogico do agente (RF12-RF15, RF21).
 *
 * <p>Fluxo: LLM -> validacao de grounding -> fallback deterministico. O aluno sempre recebe um plano.</p>
 */
@Component
public class RecoveryPlanner {

    private static final Logger log = LoggerFactory.getLogger(RecoveryPlanner.class);

    private final LlmPort llmPort;
    private final PromptFactory promptFactory;
    private final GroundedContentValidator validator;
    private final FallbackPlanner fallbackPlanner;
    private final int maxSteps;

    public RecoveryPlanner(LlmPort llmPort, PromptFactory promptFactory, GroundedContentValidator validator,
                           FallbackPlanner fallbackPlanner,
                           @Value("${educamais.agent.max-steps:5}") int maxSteps) {
        this.llmPort = llmPort;
        this.promptFactory = promptFactory;
        this.validator = validator;
        this.fallbackPlanner = fallbackPlanner;
        this.maxSteps = maxSteps;
    }

    public PlanningResult plan(RecoveryPlanRequest request) {
        long startedAt = System.currentTimeMillis();
        Optional<MissionPlan> llmPlan = llmPort.planMission(
                new PlanPrompt(promptFactory.planPrompt(request), maxSteps));

        if (llmPlan.isPresent()) {
            GroundedContentValidator.ValidationOutcome outcome =
                    validator.validate(llmPlan.get(), request.lesson().syllabus());
            if (outcome.hasValidPlan()) {
                log.info("AGENT_PLAN_SOURCE=LLM steps={} discarded={}", outcome.plan().steps().size(),
                        outcome.discardedObjectives().size());
                return new PlanningResult(outcome.plan(), outcome.discardedObjectives(),
                        System.currentTimeMillis() - startedAt, PlanSource.LLM);
            }
            log.warn("AGENT_PLAN_INVALID_LLM_OUTPUT steps={} discarded={} - aplicando fallback",
                    llmPlan.get().steps().size(), outcome.discardedObjectives().size());
            MissionPlan fallback = fallbackPlanner.plan(request);
            return new PlanningResult(fallback, outcome.discardedObjectives(),
                    System.currentTimeMillis() - startedAt, PlanSource.FALLBACK);
        } else {
            log.warn("AGENT_PLAN_LLM_UNAVAILABLE - aplicando fallback deterministico");
        }
        MissionPlan fallback = fallbackPlanner.plan(request);
        return new PlanningResult(fallback, List.of(), System.currentTimeMillis() - startedAt,
                PlanSource.FALLBACK);
    }

    /** Plano deterministico a partir da ementa (usado como complemento/garantia - RF21). */
    public MissionPlan fallback(RecoveryPlanRequest request) {
        return fallbackPlanner.plan(request);
    }

    /** Resultado do planejamento com metadados para o audit trail (RF20). */
    public record PlanningResult(MissionPlan plan, List<String> discardedObjectives, long latencyMs,
                                 PlanSource source) {
    }
}
