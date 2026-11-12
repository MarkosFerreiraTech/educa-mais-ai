package br.com.educamais.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.educamais.domain.enums.StudentStatus;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.SecurityAuditRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** RF38, RF39: anonimizacao auditada (LGPD). */
@ExtendWith(MockitoExtension.class)
class PrivacyServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SecurityAuditRepository auditRepository;

    @Test
    @DisplayName("Anonimizacao limpa PII e registra auditoria")
    void anonimiza() {
        Student student = Student.register("Ana Souza", "ana@escola.br", "+5581999998888", null, "hash",
                null, null, true);
        when(studentRepository.findById(student.id())).thenReturn(Optional.of(student));
        when(studentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PrivacyService service = new PrivacyService(studentRepository, auditRepository);
        Student anonymized = service.anonymize(student.id(), "admin-1");

        assertThat(anonymized.status()).isEqualTo(StudentStatus.ANONYMIZED);
        assertThat(anonymized.phone()).isNull();
        verify(auditRepository).record(any(), any(), any());
    }
}
