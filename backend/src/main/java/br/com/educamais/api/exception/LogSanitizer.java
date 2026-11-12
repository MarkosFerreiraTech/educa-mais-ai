package br.com.educamais.api.exception;

/**
 * Neutraliza valores controlados pelo cliente antes de irem para o log (RNF20).
 *
 * <p>Um atacante pode enviar {@code GET /api/v1/%0aFAKE_LOG_ENTRY} ou um URI com quebras de linha,
 * ANSI escapes e caracteres de controle; ao interpolar isso numa linha de log ele forja entradas
 * (log injection / log forging). Aqui mantemos apenas caracteres seguros e truncamos o tamanho,
 * de modo que o texto logado nunca quebre a linha.</p>
 */
final class LogSanitizer {

    /** Tamanho maximo de um caminho de requisicao preservado no log. */
    private static final int MAX_LENGTH = 200;

    private LogSanitizer() {
    }

    /**
     * Devolve o caminho pronto para log: sem caracteres de controle e truncado.
     *
     * @param value caminho/metodo vindo da requisicao (pode ser nulo)
     * @return valor seguro para concatenacao em log
     */
    static String path(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value
                .replaceAll("[\\p{Cntrl}]", "_")
                .replaceAll("[<>\"'`\\\\]", "_")
                .replaceAll("\\s+", "_");
        return sanitized.length() <= MAX_LENGTH ? sanitized : sanitized.substring(0, MAX_LENGTH) + "...";
    }
}
