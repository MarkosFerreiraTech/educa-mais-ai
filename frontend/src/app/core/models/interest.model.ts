/** Interesses usados na personalizacao dos exemplos (RF03, RF14). */
export interface InterestOption {
  code: string;
  label: string;
  icon: string;
  hint: string;
}

export const INTEREST_OPTIONS: InterestOption[] = [
  { code: 'futebol', label: 'Futebol', icon: 'sports', hint: 'gols, chutes e campeonatos' },
  { code: 'games', label: 'Games', icon: 'game-controller', hint: 'pontos, niveis e itens' },
  { code: 'musica', label: 'Musica', icon: 'music-note', hint: 'ritmo, compasso e playlists' },
  { code: 'ciencia', label: 'Ciencia', icon: 'flask', hint: 'experimentos e espaco' },
  { code: 'moda', label: 'Moda', icon: 'shopping-cart', hint: 'tecidos, medidas e estilo' },
  { code: 'natureza', label: 'Natureza', icon: 'leaf', hint: 'animais, clima e plantas' },
];

export function interestLabel(code: string): string {
  return INTEREST_OPTIONS.find((option) => option.code === code)?.label ?? code;
}
