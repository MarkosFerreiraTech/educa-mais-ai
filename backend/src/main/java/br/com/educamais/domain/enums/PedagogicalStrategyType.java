package br.com.educamais.domain.enums;

/**
 * Estrategias pedagogicas (padrao Strategy). A ordem define a sequencia de escalonamento
 * aplicada pelo AdaptiveStrategySelector quando o aluno erra (RF18 / RN05).
 */
public enum PedagogicalStrategyType {
    EXPLICIT(1, "explicacao direta e objetiva do conceito"),
    ANALOGY(2, "analogia concreta ligada ao interesse do aluno"),
    VISUAL(3, "representacao visual: reta numerica, diagramas e esquemas"),
    STEP_BY_STEP(4, "decomposicao em micro-passos com andaimes");

    private final int level;
    private final String approach;

    PedagogicalStrategyType(int level, String approach) {
        this.level = level;
        this.approach = approach;
    }

    public int level() {
        return level;
    }

    public String approach() {
        return approach;
    }

    /** Proxima estrategia de escalonamento; STEP_BY_STEP e o teto. */
    public PedagogicalStrategyType escalate() {
        return switch (this) {
            case EXPLICIT -> ANALOGY;
            case ANALOGY -> VISUAL;
            case VISUAL, STEP_BY_STEP -> STEP_BY_STEP;
        };
    }
}
