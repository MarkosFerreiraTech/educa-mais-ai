package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import org.springframework.stereotype.Component;

/** Reforco com andaimes: decomposicao em micro-passos verificaveis (RN05). */
@Component
public class StepByStepStrategy implements PedagogicalStrategy {

    @Override
    public PedagogicalStrategyType type() {
        return PedagogicalStrategyType.STEP_BY_STEP;
    }

    @Override
    public String instruction() {
        return "Decomponha a resolucao em micro-passos numerados (1, 2, 3), verificando o resultado "
                + "de cada etapa antes de seguir. Nao pule etapas.";
    }

    @Override
    public String templateExplanation(String objective) {
        return "Vamos passo a passo para %s. Passo 1: identifique as partes iguais. Passo 2: conte as "
                .concat("partes preenchidas. Passo 3: escreva a fracao e simplifique.")
                .formatted(objective);
    }

    @Override
    public String templateExample(String objective, String interestHint) {
        return ("Passo 1: divida uma situacao de %s em 4 partes iguais. Passo 2: conte 3 partes. "
                + "Passo 3: escreva a fracao 3/4 = 75%%.").formatted(interestHint == null ? "seu cotidiano"
                : interestHint);
    }
}
