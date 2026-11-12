package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.enums.AgentAction;
import br.com.educamais.domain.model.AgentAuditEntry;
import br.com.educamais.domain.port.AuditRepository;
import br.com.educamais.infrastructure.persistence.entity.AuditEntity;
import br.com.educamais.infrastructure.persistence.jpa.AuditJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuditPersistenceAdapter implements AuditRepository {

    private final AuditJpaRepository repository;
    private final ObjectMapper objectMapper;

    public AuditPersistenceAdapter(AuditJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentAuditEntry save(AgentAuditEntry entry) {
        return toDomain(repository.save(toEntity(entry)));
    }

    @Override
    public List<AgentAuditEntry> findByMissionId(UUID missionId) {
        return repository.findByMissionIdOrderByCreatedAtAsc(missionId).stream()
                .map(this::toDomain)
                .toList();
    }

    private AuditEntity toEntity(AgentAuditEntry entry) {
        AuditEntity entity = new AuditEntity();
        entity.setId(entry.id());
        entity.setMissionId(entry.missionId());
        entity.setAction(entry.action().name());
        entity.setDecision(entry.decision());
        entity.setDetails(writeJson(entry.details()));
        entity.setLatencyMs(entry.latencyMs());
        entity.setCreatedAt(entry.createdAt());
        return entity;
    }

    private AgentAuditEntry toDomain(AuditEntity entity) {
        return new AgentAuditEntry(entity.getId(), entity.getMissionId(), AgentAction.valueOf(entity.getAction()),
                entity.getDecision(), readJson(entity.getDetails()), entity.getLatencyMs(), entity.getCreatedAt());
    }

    private String writeJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
