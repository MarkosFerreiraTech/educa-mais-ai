package br.com.educamais.api.mapper;

import br.com.educamais.api.dto.DashboardResponse;
import br.com.educamais.api.dto.GamificationResponse;
import br.com.educamais.api.dto.LoginResponse;
import br.com.educamais.api.dto.MissionResponse;
import br.com.educamais.api.dto.NotificationResponse;
import br.com.educamais.api.dto.ProfileResponse;
import br.com.educamais.api.dto.ShopItemResponse;
import br.com.educamais.api.dto.TeacherReportResponse;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.Notification;
import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.TeacherReport;
import br.com.educamais.service.AuthService;
import br.com.educamais.service.MissionRecoveryFacade;
import br.com.educamais.service.TeacherDashboardService;
import java.util.List;

/** Conversao dominio -> DTO de resposta (P5: DTO x Entity). */
public final class ApiMapper {

    private ApiMapper() {
    }

    public static LoginResponse toLoginResponse(AuthService.AuthResult result) {
        return new LoginResponse(result.accessToken(), result.refreshToken(), result.tokenType(),
                result.expiresIn(), result.role());
    }

    public static ProfileResponse toProfile(Student student) {
        return new ProfileResponse(student.id(), student.name(), student.email(), student.phone(),
                student.status().name(),
                student.interests().stream().map(interest -> interest.code().code()).toList(),
                student.notificationOptIn());
    }

    public static GamificationResponse toGamification(GamificationProfile profile) {
        return new GamificationResponse(profile.studentId(), profile.totalXp(), profile.level(),
                profile.xpToNextLevel(), profile.diamonds(), profile.coins(), profile.currentStreak(),
                profile.longestStreak());
    }

    public static MissionResponse toMission(Mission mission, DiagnosticQuestion question) {
        List<MissionResponse.StepResponse> steps = mission.steps().stream()
                .map(step -> new MissionResponse.StepResponse(step.order(), step.objective(), step.explanation(),
                        step.example(), step.strategy().name(), step.status().name(), step.attempts()))
                .toList();
        MissionResponse.QuestionResponse questionResponse = question == null ? null
                : new MissionResponse.QuestionResponse(question.id(), question.prompt(), question.type().name(),
                        question.options(), question.difficulty());
        return new MissionResponse(mission.id(), mission.status().name(), mission.planSource().name(),
                mission.progressPercent(), mission.totalAttempts(), mission.isFinished(), mission.createdAt(),
                mission.completedAt(), steps, questionResponse);
    }

    public static MissionResponse toJourney(MissionRecoveryFacade.MissionJourney journey) {
        return toMission(journey.mission(), journey.question());
    }

    public static NotificationResponse toNotification(Notification notification) {
        return new NotificationResponse(notification.id(), notification.channel().name(),
                notification.status().name(), notification.payload(), notification.failureReason(),
                notification.sentAt());
    }

    public static TeacherReportResponse toReport(TeacherReport report) {
        return new TeacherReportResponse(report.id(), report.missionId(), report.studentId(), report.summary(),
                report.identifiedGaps(), report.recommendation(), report.successRate(),
                report.deliveryStatus(), report.createdAt());
    }

    public static ShopItemResponse toShopItem(ShopItem item, boolean owned) {
        return new ShopItemResponse(item.code(), item.name(), item.category(), item.priceCoins(),
                item.assetUrl(), owned);
    }

    public static DashboardResponse toDashboard(TeacherDashboardService.ClassroomDashboard dashboard) {
        List<DashboardResponse.StudentRow> rows = dashboard.students().stream()
                .map(row -> new DashboardResponse.StudentRow(row.studentId(), row.firstName(), row.status(),
                        row.pendingMissions(), row.completedMissions(), row.stepsNeedingSupport(),
                        row.reportsAvailable()))
                .toList();
        return new DashboardResponse(dashboard.classroomId(), dashboard.totalStudents(),
                dashboard.pendingMissions(), dashboard.completedMissions(), dashboard.completionRate(),
                dashboard.reachableByWhatsApp(), rows);
    }
}
