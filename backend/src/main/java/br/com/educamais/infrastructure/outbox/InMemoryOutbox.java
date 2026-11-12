package br.com.educamais.infrastructure.outbox;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Implementacao local do outbox (em nuvem: tabela + scheduler ou SQS DLQ). */
@Component
public class InMemoryOutbox implements OutboxPort {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Override
    public void store(String eventType, String payload) {
        events.add(eventType + ":" + payload);
    }

    @Override
    public long pendingCount() {
        return events.size();
    }
}
