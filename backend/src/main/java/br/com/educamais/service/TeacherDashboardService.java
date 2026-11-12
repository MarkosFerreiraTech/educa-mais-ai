package br.com.educamais.service;

import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.MissionRepository;
import br.com.educamais.domain.port.ReportRepository;
import br.com.educamais.domain.port.StudentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Dashboard do professor (RF34 / UC10). */
@Service
public class TeacherDashboardService {

    private final StudentRepository studentRepository;
    private final MissionRepository missionRepository;
    private final ReportRepository reportRepository;

    public TeacherDashboardService(StudentRepository studentRepository, MissionRepository missionRepository,
                                   ReportRepository reportRepository) {
        this.studentRepository = studentRepository;
        this.missionRepository = missionRepository;
        this.reportRepository = reportRepository;
    }

    public ClassroomDashboard dashboard(UUID classroomId) {
        List<Student> students = studentRepository.findByClassroomId(classroomId);
        List<ClassroomDashboard.StudentRow> rows = students.stream()
                .map(student -> toRow(student))
                .sorted(Comparator.comparingInt(ClassroomDashboard.StudentRow::pendingMissions).reversed())
                .toList();

        int pending = rows.stream().mapToInt(ClassroomDashboard.StudentRow::pendingMissions).sum();
        int completed = rows.stream().mapToInt(ClassroomDashboard.StudentRow::completedMissions).sum();
        int totalMissions = pending + completed;
        int completionRate = totalMissions == 0 ? 0 : (completed * 100) / totalMissions;

        return new ClassroomDashboard(classroomId, students.size(), pending, completed, completionRate,
                students.stream().filter(student -> student.phone() != null).count(), rows);
    }

    private ClassroomDashboard.StudentRow toRow(Student student) {
        List<Mission> missions = missionRepository.findByStudentId(student.id());
        int pending = (int) missions.stream()
                .filter(mission -> mission.status() == MissionStatus.NOTIFIED
                        || mission.status() == MissionStatus.IN_PROGRESS
                        || mission.status() == MissionStatus.PLANNED)
                .count();
        int completed = (int) missions.stream().filter(Mission::isFinished).count();
        int needsSupport = (int) missions.stream()
                .flatMap(mission -> mission.steps().stream())
                .filter(step -> step.status() == br.com.educamais.domain.enums.StepStatus.NEEDS_REINFORCEMENT)
                .count();
        long reports = reportRepository.findByStudentId(student.id()).size();
        return new ClassroomDashboard.StudentRow(student.id(), student.firstName(), student.status().name(),
                pending, completed, needsSupport, reports);
    }

    /** Payload do dashboard da turma. */
    public record ClassroomDashboard(UUID classroomId, int totalStudents, int pendingMissions,
                                     int completedMissions, int completionRate, long reachableByWhatsApp,
                                     List<StudentRow> students) {

        /** Linha por aluno. */
        public record StudentRow(UUID studentId, String firstName, String status, int pendingMissions,
                                 int completedMissions, int stepsNeedingSupport, long reportsAvailable) {
        }
    }
}
