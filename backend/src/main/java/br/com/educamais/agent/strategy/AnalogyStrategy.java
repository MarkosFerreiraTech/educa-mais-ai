package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import org.springframework.stereotype.Component;

/** Abordagem por analogia ancorada no interesse pessoal do aluno (RF14). */
@Component
public class AnalogyStrategy implements PedagogicalStrategy {

    @Override
    public PedagogicalStrategyType type() {
        return PedagogicalStrategyType.ANALOGY;
    }

    @Override
    public String instruction() {
        return "Explique por meio de uma analogia concreta ligada ao interesse do aluno, "
                + "mapeando cada elemento da analogia para o conceito matematico.";
    }

    @Override
    public String templateExplanation(String objective) {
        return "Pense no seu interesse favorito: o objetivo (%s) aparece o tempo todo nele. "
                .concat("Vamos traduzir o conceito para algo que voce ja vive no dia a dia.")
                .formatted(objective);
    }

    @Override
    public String templateExample(String objective, String interestHint) {
        return "Se voce acertou 3 de 4 pendencias em %s, voce completou 3/4 = 75%% do total.".formatted(
                interestHint == null ? "seu esporte" : interestHint);
    }
}
