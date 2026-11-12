package br.com.educamais.service;

import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.GamificationRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados do aluno e perfil de gamificacao (RF05, RF32). */
@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final GamificationRepository gamificationRepository;

    public StudentService(StudentRepository studentRepository, GamificationRepository gamificationRepository) {
        this.studentRepository = studentRepository;
        this.gamificationRepository = gamificationRepository;
    }

    public Student get(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentId));
    }

    public GamificationProfile profileOf(UUID studentId) {
        return gamificationRepository.findByStudentId(studentId)
                .orElseGet(() -> gamificationRepository.save(GamificationProfile.newFor(studentId)));
    }

    @Transactional
    public Student updatePreferences(UUID studentId, boolean whatsAppOptIn, String phone) {
        Student student = get(studentId);
        student.updateNotificationPreferences(whatsAppOptIn);
        student.updateContact(phone);
        return studentRepository.save(student);
    }

    public List<Student> byClassroom(UUID classroomId) {
        return studentRepository.findByClassroomId(classroomId);
    }
}
