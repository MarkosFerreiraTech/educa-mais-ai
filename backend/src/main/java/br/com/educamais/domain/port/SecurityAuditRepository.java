package br.com.educamais.domain.port;

/** Trilha de auditoria de eventos de seguranca (RF39 -- sempre sem PII). */
public interface SecurityAuditRepository {

    void record(String event, String subjectReference, String detail);
}
