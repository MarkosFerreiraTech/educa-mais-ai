package br.com.educamais.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.educamais.domain.port.AttendanceRepository;
import br.com.educamais.domain.port.ClockPort;
import br.com.educamais.domain.port.LessonRepository;
import br.com.educamais.domain.port.LlmPort;
import br.com.educamais.domain.port.MissionRepository;
import br.com.educamais.domain.port.NotificationPort;
import br.com.educamais.domain.port.PasswordHasherPort;
import br.com.educamais.domain.port.StudentRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RNF24: as portas de dominio sao contratos (interfaces), nunca implementacoes. */
class PortsAreInterfacesTest {

    @Test
    @DisplayName("Todos os ports do dominio sao interfaces")
    void portsSaoInterfaces() {
        List<Class<?>> ports = List.of(StudentRepository.class, LessonRepository.class,
                AttendanceRepository.class, MissionRepository.class, LlmPort.class, NotificationPort.class,
                ClockPort.class, PasswordHasherPort.class);
        assertThat(ports).allMatch(Class::isInterface);
    }
}
