package br.com.educamais.infrastructure.outbox;

/** Registro de eventos para reprocessamento (padrao outbox -- RNF32). */
public interface OutboxPort {

    void store(String eventType, String payload);

    long pendingCount();
}
