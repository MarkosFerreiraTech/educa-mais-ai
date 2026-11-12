package br.com.educamais.service;

import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.SecurityAuditRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Direito ao esquecimento / anonimizacao (RF38, RF39 / LGPD). */
@Service
public class PrivacyService {

    private final StudentRepository studentRepository;
    private final SecurityAuditRepository auditRepository;

    public PrivacyService(StudentRepository studentRepository, SecurityAuditRepository auditRepository) {
        this.studentRepository = studentRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public Student anonymize(UUID studentId, String requestedBy) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentId));
        student.anonymize();
        Student saved = studentRepository.save(student);
        auditRepository.record("privacy.anonymized", "student:" + studentId, "requestedBy=" + requestedBy);
        return saved;
    }
}
