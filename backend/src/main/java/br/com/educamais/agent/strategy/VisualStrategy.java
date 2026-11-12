package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import org.springframework.stereotype.Component;

/** Abordagem visual: reta numerica, divisao em partes e esquemas. */
@Component
public class VisualStrategy implements PedagogicalStrategy {

    @Override
    public PedagogicalStrategyType type() {
        return PedagogicalStrategyType.VISUAL;
    }

    @Override
    public String instruction() {
        return "Descreva uma representacao visual em texto (reta numerica, barra dividida ou esquema) "
                + "explicitando as partes e a posicao dos numeros.";
    }

    @Override
    public String templateExplanation(String objective) {
        return "Imagine uma barra dividida em partes iguais. Para %s, marcamos cada parte na barra "
                .concat("e contamos quantas preenchemos: assim a fracao fica visivel.")
                .formatted(objective);
    }

    @Override
    public String templateExample(String objective, String interestHint) {
        return ("Desenhe uma reta de 0 a 1 com 4 marcas pensando em %s: cada marca e 1/4 e duas marcas "
                + "equivalem a 2/4 = 1/2.").formatted(interestHint == null ? "uma situacao do dia a dia"
                : interestHint);
    }
}
