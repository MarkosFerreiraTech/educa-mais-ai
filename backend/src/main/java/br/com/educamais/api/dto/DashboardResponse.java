package br.com.educamais.api.dto;

import java.util.List;
import java.util.UUID;

public record DashboardResponse(UUID classroomId, int totalStudents, int pendingMissions, int completedMissions,
                                int completionRate, long reachableByWhatsApp, List<StudentRow> students) {

    public record StudentRow(UUID studentId, String firstName, String status, int pendingMissions,
                             int completedMissions, int stepsNeedingSupport, long reportsAvailable) {
    }
}
