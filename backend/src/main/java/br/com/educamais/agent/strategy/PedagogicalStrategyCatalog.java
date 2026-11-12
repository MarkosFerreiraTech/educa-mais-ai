package br.com.educamais.agent.strategy;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Registro das estrategias disponiveis (Open/Closed: novas abordagens entram aqui). */
@Component
public class PedagogicalStrategyCatalog {

    private final Map<PedagogicalStrategyType, PedagogicalStrategy> strategies =
            new EnumMap<>(PedagogicalStrategyType.class);

    public PedagogicalStrategyCatalog(List<PedagogicalStrategy> available) {
        available.forEach(strategy -> strategies.put(strategy.type(), strategy));
    }

    public PedagogicalStrategy get(PedagogicalStrategyType type) {
        PedagogicalStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Estrategia pedagogica nao registrada: " + type);
        }
        return strategy;
    }
}
