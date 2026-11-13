/** Interesses usados na personalizacao dos exemplos (RF03, RF14). */
export interface InterestOption {
  code: string;
  label: string;
  /** Emoji exibido no chip (mesmo vocabulario visual do onboarding). */
  icon: string;
  hint: string;
}

export const INTEREST_OPTIONS: InterestOption[] = [
  { code: 'futebol', label: 'Futebol', icon: '⚽', hint: 'gols, chutes e campeonatos' },
  { code: 'games', label: 'Games', icon: '🎮', hint: 'pontos, niveis e itens' },
  { code: 'musica', label: 'Musica', icon: '🎵', hint: 'ritmo, compasso e playlists' },
  { code: 'ciencia', label: 'Ciencia', icon: '🔬', hint: 'experimentos e espaco' },
  { code: 'moda', label: 'Moda', icon: '👗', hint: 'tecidos, medidas e estilo' },
  { code: 'natureza', label: 'Natureza', icon: '🌱', hint: 'animais, clima e plantas' },
];

export function interestLabel(code: string): string {
  return INTEREST_OPTIONS.find((option) => option.code === code)?.label ?? code;
}

export function interestIcon(code: string): string {
  return INTEREST_OPTIONS.find((option) => option.code === code)?.icon ?? '✨';
}
