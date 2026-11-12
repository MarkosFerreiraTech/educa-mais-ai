package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.port.SecurityAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Auditoria de seguranca em log estruturado (sem PII).
 * Em producao, emite metrica/evento para CloudWatch; localmente, log JSON.
 */
@Component
public class SecurityAuditPersistenceAdapter implements SecurityAuditRepository {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditPersistenceAdapter.class);

    @Override
    public void record(String event, String subjectReference, String detail) {
        log.warn("SECURITY_AUDIT event={} subjectRef={} detail={}", event, subjectReference, detail);
    }
}
