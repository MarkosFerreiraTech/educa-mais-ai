package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.model.MissionStep;
import org.springframework.stereotype.Component;

/**
 * Decide a proxima abordagem pedagogica a partir do historico de tentativas (RF18 / RN05).
 */
@Component
public class AdaptiveStrategySelector {

    public PedagogicalStrategyType nextStrategy(MissionStep step) {
        if (step.attempts() >= MissionStep.MAX_ATTEMPTS) {
            return PedagogicalStrategyType.STEP_BY_STEP;
        }
        PedagogicalStrategyType escalated = step.strategy().escalate();
        return escalated == step.strategy() ? PedagogicalStrategyType.STEP_BY_STEP : escalated;
    }

    public boolean isReinforcement(PedagogicalStrategyType type) {
        return type == PedagogicalStrategyType.STEP_BY_STEP;
    }
}
