package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;

/**
 * Strategy: cada abordagem pedagogica sabe se apresentar e como orientar o LLM (RF18).
 */
public interface PedagogicalStrategy {

    PedagogicalStrategyType type();

    String instruction();

    String templateExplanation(String objective);

    String templateExample(String objective, String interestHint);
}
