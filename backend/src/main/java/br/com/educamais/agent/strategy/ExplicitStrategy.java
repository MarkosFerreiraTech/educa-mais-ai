package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import org.springframework.stereotype.Component;

/** Abordagem direta: definicao + regra + exemplo minimo. */
@Component
public class ExplicitStrategy implements PedagogicalStrategy {

    @Override
    public PedagogicalStrategyType type() {
        return PedagogicalStrategyType.EXPLICIT;
    }

    @Override
    public String instruction() {
        return "Explique o conceito de forma direta e objetiva, com uma definicao curta, "
                + "a regra principal e um exemplo numerico simples. Use frases curtas.";
    }

    @Override
    public String templateExplanation(String objective) {
        return "Vamos direto ao ponto: o objetivo desta aula e %s. A ideia central e dividir o "
                .concat("problema em partes iguais e comparar essas partes.")
                .formatted(objective);
    }

    @Override
    public String templateExample(String objective, String interestHint) {
        return "Exemplo rapido sobre %s: se o time fez 2 gols em 4 chutes, o aproveitamento e 2/4.".formatted(
                interestHint == null ? "o tema" : interestHint);
    }
}
