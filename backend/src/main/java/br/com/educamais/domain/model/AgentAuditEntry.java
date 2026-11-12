package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.AgentAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Registro auditavel de cada decisao do agente (RF20). */
public record AgentAuditEntry(UUID id, UUID missionId, AgentAction action, String decision,
                              Map<String, Object> details, int latencyMs, Instant createdAt) {

    public AgentAuditEntry {
        id = id == null ? UUID.randomUUID() : id;
        details = details == null ? Map.of() : Map.copyOf(details);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static AgentAuditEntry of(UUID missionId, AgentAction action, String decision,
                                     Map<String, Object> details, int latencyMs) {
        return new AgentAuditEntry(null, missionId, action, decision, details, latencyMs, Instant.now());
    }
}
