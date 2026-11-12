package br.com.educamais.agent.audit;

import br.com.educamais.domain.enums.AgentAction;
import br.com.educamais.domain.model.AgentAuditEntry;
import br.com.educamais.domain.port.AuditRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Registra cada decisao do ciclo autonomo (RF20 / RNF30). */
@Component
public class AgentAuditTrail {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditTrail.class);

    private final AuditRepository auditRepository;

    public AgentAuditTrail(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public AgentAuditEntry record(UUID missionId, AgentAction action, String decision,
                                  Map<String, Object> details, long latencyMs) {
        log.info("AGENT_AUDIT missionRef={} action={} decision={} latencyMs={}", missionId, action, decision,
                latencyMs);
        return auditRepository.save(AgentAuditEntry.of(missionId, action, decision, details, (int) latencyMs));
    }

    public List<AgentAuditEntry> history(UUID missionId) {
        return auditRepository.findByMissionId(missionId);
    }
}
