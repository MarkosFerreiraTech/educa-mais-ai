package br.com.educamais.domain.port;

import br.com.educamais.domain.model.AgentAuditEntry;
import java.util.List;
import java.util.UUID;

public interface AuditRepository {

    AgentAuditEntry save(AgentAuditEntry entry);

    List<AgentAuditEntry> findByMissionId(UUID missionId);
}
