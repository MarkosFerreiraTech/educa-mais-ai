/** Rotulos amigaveis das estrategias pedagogicas do agente (RF18). */
export const STRATEGY_LABELS: Record<string, string> = {
  EXPLICIT: 'Explicacao direta',
  ANALOGY: 'Analogia com seu interesse',
  VISUAL: 'Representacao visual',
  STEP_BY_STEP: 'Passo a passo com apoio',
};

export const STRATEGY_HINTS: Record<string, string> = {
  EXPLICIT: 'O tutor explicou o conceito do jeito mais direto.',
  ANALOGY: 'O tutor usou uma analogia ligada ao que voce gosta.',
  VISUAL: 'O tutor desenhou o conceito em texto: reta numerica e partes.',
  STEP_BY_STEP: 'O tutor quebrou o problema em micro-passos para voce acompanhar.',
};

export function strategyLabel(strategy: string): string {
  return STRATEGY_LABELS[strategy] ?? strategy;
}

export function strategyHint(strategy: string): string {
  return STRATEGY_HINTS[strategy] ?? '';
}
