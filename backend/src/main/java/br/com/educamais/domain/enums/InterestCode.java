package br.com.educamais.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/** Interesses pessoais usados para contextualizar explicacoes e exemplos. */
public enum InterestCode {
    FOOTBALL("futebol", "Futebol", "jogos, gols, campeonatos e medidas de campo"),
    GAMES("games", "Games", "pontuacao, niveis, itens e estrategias de jogo"),
    MUSIC("musica", "Musica", "ritmo, compasso, frequencias e playlists"),
    SCIENCE("ciencia", "Ciencia", "experimentos, espaco e natureza"),
    FASHION("moda", "Moda", "tecidos, medidas, estampas e combinacoes"),
    NATURE("natureza", "Natureza", "animais, plantas, clima e ecossistemas");

    private final String code;
    private final String label;
    private final String contextHint;

    InterestCode(String code, String label, String contextHint) {
        this.code = code;
        this.label = label;
        this.contextHint = contextHint;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String contextHint() {
        return contextHint;
    }

    public static Optional<InterestCode> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code) || value.name().equalsIgnoreCase(code))
                .findFirst();
    }
}
