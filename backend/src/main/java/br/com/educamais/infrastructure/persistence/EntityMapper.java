package br.com.educamais.infrastructure.persistence;

import br.com.educamais.crypto.CryptoSupport;
import br.com.educamais.domain.enums.AnswerClassification;
import br.com.educamais.domain.enums.AttendanceOrigin;
import br.com.educamais.domain.enums.AttendanceStatus;
import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.enums.MissionStatus;
import br.com.educamais.domain.enums.NotificationChannel;
import br.com.educamais.domain.enums.NotificationStatus;
import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.enums.QuestionType;
import br.com.educamais.domain.enums.RewardType;
import br.com.educamais.domain.enums.StepStatus;
import br.com.educamais.domain.enums.StudentStatus;
import br.com.educamais.domain.model.Attendance;
import br.com.educamais.domain.model.DiagnosticAttempt;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.GamificationProfile;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Lesson;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionStep;
import br.com.educamais.domain.model.Notification;
import br.com.educamais.domain.model.RewardEntry;
import br.com.educamais.domain.model.ShopItem;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.model.Syllabus;
import br.com.educamais.domain.model.TeacherReport;
import br.com.educamais.infrastructure.persistence.entity.AttemptEntity;
import br.com.educamais.infrastructure.persistence.entity.AttendanceEntity;
import br.com.educamais.infrastructure.persistence.entity.GamificationEntity;
import br.com.educamais.infrastructure.persistence.entity.LessonEntity;
import br.com.educamais.infrastructure.persistence.entity.MissionEntity;
import br.com.educamais.infrastructure.persistence.entity.MissionStepEntity;
import br.com.educamais.infrastructure.persistence.entity.NotificationEntity;
import br.com.educamais.infrastructure.persistence.entity.QuestionEntity;
import br.com.educamais.infrastructure.persistence.entity.ReportEntity;
import br.com.educamais.infrastructure.persistence.entity.RewardEntity;
import br.com.educamais.infrastructure.persistence.entity.ShopItemEntity;
import br.com.educamais.infrastructure.persistence.entity.StudentEntity;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Mapeamento explicito entre entidades JPA e o modelo de dominio (P5: DTO x Entity).
 * Interesses sao serializados como {@code CODE:priority,CODE:priority}.
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    /* ------------------------------- Student ------------------------------- */

    public static StudentEntity toEntity(Student student) {
        return toEntity(new StudentEntity(), student);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static StudentEntity toEntity(StudentEntity entity, Student student) {
        entity.setId(student.id() == null ? UUID.randomUUID() : student.id());
        entity.setName(student.name());
        entity.setEmail(student.email());
        entity.setEmailHash(CryptoSupport.encryptor().blindIndex(student.email()));
        entity.setPhone(student.phone());
        entity.setBirthDate(student.birthDate());
        entity.setPasswordHash(student.passwordHash());
        entity.setSchoolId(student.schoolId());
        entity.setClassroomId(student.classroomId());
        entity.setStatus(student.status().name());
        entity.setNotificationOptIn(student.notificationOptIn());
        entity.setInterests(serializeInterests(student.interests()));
        return entity;
    }

    public static Student toDomain(StudentEntity entity) {
        Student student = new Student(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.birthDateAsLocalDate(),
                entity.getPasswordHash(),
                entity.getSchoolId(),
                entity.getClassroomId(),
                StudentStatus.valueOf(entity.getStatus()),
                entity.isNotificationOptIn());
        student.defineInterests(deserializeInterests(entity.getInterests()));
        return student;
    }

    public static String serializeInterests(List<Interest> interests) {
        if (interests == null || interests.isEmpty()) {
            return null;
        }
        return interests.stream()
                .map(interest -> interest.code().name() + ":" + interest.priority())
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    public static List<Interest> deserializeInterests(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> part.contains(":"))
                .map(part -> {
                    String[] pair = part.split(":");
                    InterestCode code = InterestCode.valueOf(pair[0]);
                    return Interest.of(code, Integer.parseInt(pair[1]));
                })
                .toList();
    }

    /* ------------------------------- Lesson -------------------------------- */

    public static LessonEntity toEntity(Lesson lesson) {
        return toEntity(new LessonEntity(), lesson);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static LessonEntity toEntity(LessonEntity entity, Lesson lesson) {
        entity.setId(lesson.id() == null ? UUID.randomUUID() : lesson.id());
        entity.setClassroomId(lesson.classroomId());
        entity.setSubject(lesson.subject());
        entity.setTopic(lesson.topic());
        entity.setLessonDate(lesson.lessonDate());
        entity.setLearningObjectives(join(lesson.syllabus().learningObjectives()));
        entity.setTopics(join(lesson.syllabus().topics()));
        entity.setPrerequisites(join(lesson.syllabus().prerequisites()));
        entity.setSourceReference(lesson.syllabus().sourceReference());
        return entity;
    }

    public static Lesson toDomain(LessonEntity entity) {
        return new Lesson(entity.getId(), entity.getClassroomId(), entity.getSubject(), entity.getTopic(),
                entity.getLessonDate(),
                new Syllabus(split(entity.getLearningObjectives()), split(entity.getTopics()),
                        split(entity.getPrerequisites()), entity.getSourceReference()));
    }

    /* ------------------------------ Attendance ----------------------------- */

    public static AttendanceEntity toEntity(Attendance attendance) {
        return toEntity(new AttendanceEntity(), attendance);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static AttendanceEntity toEntity(AttendanceEntity entity, Attendance attendance) {
        entity.setId(attendance.id() == null ? UUID.randomUUID() : attendance.id());
        entity.setStudentId(attendance.studentId());
        entity.setLessonId(attendance.lessonId());
        entity.setEventId(attendance.eventId());
        entity.setStatus(attendance.status().name());
        entity.setOrigin(attendance.origin().name());
        entity.setOccurredAt(attendance.occurredAt());
        return entity;
    }

    public static Attendance toDomain(AttendanceEntity entity) {
        return new Attendance(entity.getId(), entity.getStudentId(), entity.getLessonId(), entity.getEventId(),
                AttendanceStatus.valueOf(entity.getStatus()), AttendanceOrigin.valueOf(entity.getOrigin()),
                entity.getOccurredAt());
    }

    /* -------------------------------- Mission ------------------------------ */

    public static MissionEntity toEntity(Mission mission) {
        return toEntity(new MissionEntity(), mission);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static MissionEntity toEntity(MissionEntity entity, Mission mission) {
        entity.setId(mission.id() == null ? UUID.randomUUID() : mission.id());
        entity.setAttendanceId(mission.attendanceId());
        entity.setStudentId(mission.studentId());
        entity.setLessonId(mission.lessonId());
        entity.setStatus(mission.status().name());
        entity.setPlanSource(mission.planSource().name());
        entity.setCreatedAt(mission.createdAt());
        entity.setCompletedAt(mission.completedAt());
        for (MissionStep step : mission.steps()) {
            entity.addStep(toEntity(step));
        }
        return entity;
    }

    public static MissionStepEntity toEntity(MissionStep step) {
        return toEntity(new MissionStepEntity(), step);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static MissionStepEntity toEntity(MissionStepEntity entity, MissionStep step) {
        entity.setId(step.id() == null ? UUID.randomUUID() : step.id());
        entity.setStepOrder(step.order());
        entity.setObjective(step.objective());
        entity.setExplanation(step.explanation());
        entity.setExample(step.example());
        entity.setStrategy(step.strategy().name());
        entity.setStatus(step.status().name());
        entity.setAttempts(step.attempts());
        return entity;
    }

    public static Mission toDomain(MissionEntity entity) {
        List<MissionStep> steps = entity.getSteps().stream()
                .map(EntityMapper::toDomain)
                .toList();
        return new Mission(entity.getId(), entity.getAttendanceId(), entity.getStudentId(), entity.getLessonId(),
                MissionStatus.valueOf(entity.getStatus()), PlanSource.valueOf(entity.getPlanSource()),
                steps, entity.getCreatedAt(), entity.getCompletedAt());
    }

    public static MissionStep toDomain(MissionStepEntity entity) {
        return new MissionStep(entity.getId(), entity.getStepOrder(), entity.getObjective(),
                entity.getExplanation(), entity.getExample(),
                PedagogicalStrategyType.valueOf(entity.getStrategy()),
                StepStatus.valueOf(entity.getStatus()), entity.getAttempts());
    }

    /* ----------------------------- Gamification ---------------------------- */

    public static GamificationEntity toEntity(GamificationProfile profile) {
        return toEntity(new GamificationEntity(), profile);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static GamificationEntity toEntity(GamificationEntity entity, GamificationProfile profile) {
        entity.setStudentId(profile.studentId());
        entity.setTotalXp(profile.totalXp());
        entity.setDiamonds(profile.diamonds());
        entity.setCoins(profile.coins());
        entity.setCurrentStreak(profile.currentStreak());
        entity.setLongestStreak(profile.longestStreak());
        entity.setLastStreakDate(profile.lastStreakDate());
        return entity;
    }

    public static GamificationProfile toDomain(GamificationEntity entity) {
        return new GamificationProfile(entity.getStudentId(), entity.getTotalXp(), entity.getDiamonds(),
                entity.getCoins(), entity.getCurrentStreak(), entity.getLongestStreak(),
                entity.getLastStreakDate());
    }

    /* ------------------------------- Reward -------------------------------- */

    public static RewardEntity toEntity(RewardEntry entry) {
        return toEntity(new RewardEntity(), entry);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static RewardEntity toEntity(RewardEntity entity, RewardEntry entry) {
        entity.setId(entry.id() == null ? UUID.randomUUID() : entry.id());
        entity.setStudentId(entry.studentId());
        entity.setMissionId(entry.missionId());
        entity.setType(entry.type().name());
        entity.setAmount(entry.amount());
        entity.setIdempotencyKey(entry.idempotencyKey());
        entity.setGrantedAt(entry.grantedAt());
        return entity;
    }

    public static RewardEntry toDomain(RewardEntity entity) {
        return new RewardEntry(entity.getId(), entity.getStudentId(), entity.getMissionId(),
                RewardType.valueOf(entity.getType()), entity.getAmount(), entity.getIdempotencyKey(),
                entity.getGrantedAt());
    }

    /* ---------------------------- Notification ----------------------------- */

    public static NotificationEntity toEntity(Notification notification) {
        return toEntity(new NotificationEntity(), notification);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static NotificationEntity toEntity(NotificationEntity entity, Notification notification) {
        entity.setId(notification.id() == null ? UUID.randomUUID() : notification.id());
        entity.setStudentId(notification.studentId());
        entity.setMissionId(notification.missionId());
        entity.setChannel(notification.channel().name());
        entity.setStatus(notification.status().name());
        entity.setProviderMessageId(notification.providerMessageId());
        entity.setFailureReason(notification.failureReason());
        entity.setPayload(notification.payload());
        entity.setSentAt(notification.sentAt());
        return entity;
    }

    public static Notification toDomain(NotificationEntity entity) {
        Notification notification = new Notification(entity.getId(), entity.getStudentId(), entity.getMissionId(),
                NotificationChannel.valueOf(entity.getChannel()), NotificationStatus.valueOf(entity.getStatus()),
                entity.getPayload());
        if (entity.getStatus().equals(NotificationStatus.SENT.name())) {
            notification.markSent(entity.getProviderMessageId());
        } else if (entity.getStatus().equals(NotificationStatus.FAILED.name())) {
            notification.markFailed(entity.getFailureReason());
        } else if (entity.getStatus().equals(NotificationStatus.SKIPPED.name())) {
            notification.markSkipped(entity.getFailureReason());
        }
        return notification;
    }

    /* -------------------------------- Report ------------------------------- */

    public static ReportEntity toEntity(TeacherReport report) {
        return toEntity(new ReportEntity(), report);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static ReportEntity toEntity(ReportEntity entity, TeacherReport report) {
        entity.setId(report.id() == null ? UUID.randomUUID() : report.id());
        entity.setMissionId(report.missionId());
        entity.setStudentId(report.studentId());
        entity.setTeacherId(report.teacherId());
        entity.setSummary(report.summary());
        entity.setIdentifiedGaps(join(report.identifiedGaps()));
        entity.setRecommendation(report.recommendation());
        entity.setSuccessRate(report.successRate());
        entity.setDeliveryStatus(report.deliveryStatus());
        entity.setCreatedAt(report.createdAt());
        return entity;
    }

    public static TeacherReport toDomain(ReportEntity entity) {
        return new TeacherReport(entity.getId(), entity.getMissionId(), entity.getStudentId(),
                entity.getTeacherId(), entity.getSummary(), split(entity.getIdentifiedGaps()),
                entity.getRecommendation(), entity.getSuccessRate(), entity.getDeliveryStatus(),
                entity.getCreatedAt());
    }

    /* ------------------------------ Question ------------------------------- */

    public static QuestionEntity toEntity(DiagnosticQuestion question) {
        return toEntity(new QuestionEntity(), question);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static QuestionEntity toEntity(QuestionEntity entity, DiagnosticQuestion question) {
        entity.setId(question.id() == null ? UUID.randomUUID() : question.id());
        entity.setMissionId(question.missionId());
        entity.setStepId(question.stepId());
        entity.setPrompt(question.prompt());
        entity.setType(question.type().name());
        entity.setOptions(join(question.options()));
        entity.setCorrectAnswer(question.correctAnswer());
        entity.setExplanation(question.explanation());
        entity.setDifficulty(question.difficulty());
        return entity;
    }

    public static DiagnosticQuestion toDomain(QuestionEntity entity) {
        return new DiagnosticQuestion(entity.getId(), entity.getMissionId(), entity.getStepId(),
                entity.getPrompt(), QuestionType.valueOf(entity.getType()), split(entity.getOptions()),
                entity.getCorrectAnswer(), entity.getExplanation(), entity.getDifficulty());
    }

    /* ------------------------------- Attempt ------------------------------- */

    public static AttemptEntity toEntity(DiagnosticAttempt attempt) {
        return toEntity(new AttemptEntity(), attempt);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static AttemptEntity toEntity(AttemptEntity entity, DiagnosticAttempt attempt) {
        entity.setId(attempt.id() == null ? UUID.randomUUID() : attempt.id());
        entity.setQuestionId(attempt.questionId());
        entity.setStudentId(attempt.studentId());
        entity.setAnswer(attempt.answer());
        entity.setClassification(attempt.classification().name());
        entity.setFeedback(attempt.feedback());
        entity.setAnsweredAt(attempt.answeredAt());
        entity.setTimeSpentSeconds(attempt.timeSpentSeconds());
        return entity;
    }

    public static DiagnosticAttempt toDomain(AttemptEntity entity) {
        return new DiagnosticAttempt(entity.getId(), entity.getQuestionId(), entity.getStudentId(),
                entity.getAnswer(), AnswerClassification.valueOf(entity.getClassification()),
                entity.getFeedback(), entity.getAnsweredAt(), entity.getTimeSpentSeconds());
    }

    /* ------------------------------- Shop ---------------------------------- */

    public static ShopItemEntity toEntity(ShopItem item) {
        return toEntity(new ShopItemEntity(), item);
    }

    /** Reaproveita a instancia gerenciada pelo JPA e apenas atualiza os campos. */
    public static ShopItemEntity toEntity(ShopItemEntity entity, ShopItem item) {
        entity.setId(item.id() == null ? UUID.randomUUID() : item.id());
        entity.setCode(item.code());
        entity.setName(item.name());
        entity.setCategory(item.category());
        entity.setPriceCoins(item.priceCoins());
        entity.setAssetUrl(item.assetUrl());
        return entity;
    }

    public static ShopItem toDomain(ShopItemEntity entity) {
        return new ShopItem(entity.getId(), entity.getCode(), entity.getName(), entity.getCategory(),
                entity.getPriceCoins(), entity.getAssetUrl());
    }

    /* ------------------------------- Utils --------------------------------- */

    public static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("|", values);
    }

    public static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    public static LocalDate parseDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}
