package br.com.educamais.domain.model;

import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.enums.StudentStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aluno -- entidade rica do dominio. Nao conhece JPA nem HTTP (DIP / hexagonal).
 */
public class Student {

    private static final InterestCode DEFAULT_INTEREST = InterestCode.NATURE;

    private final UUID id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String passwordHash;
    private UUID schoolId;
    private UUID classroomId;
    private StudentStatus status;
    private boolean notificationOptIn;
    private final List<Interest> interests = new ArrayList<>();

    public Student(UUID id, String name, String email, String phone, LocalDate birthDate,
                   String passwordHash, UUID schoolId, UUID classroomId, StudentStatus status,
                   boolean notificationOptIn) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.name = requireText(name, "Nome do aluno");
        this.email = normalizeEmail(email);
        this.phone = phone;
        this.birthDate = birthDate;
        this.passwordHash = passwordHash;
        this.schoolId = schoolId;
        this.classroomId = classroomId;
        this.status = status == null ? StudentStatus.PENDING_ONBOARDING : status;
        this.notificationOptIn = notificationOptIn;
    }

    public static Student register(String name, String email, String phone, LocalDate birthDate,
                                   String passwordHash, UUID schoolId, UUID classroomId,
                                   boolean notificationOptIn) {
        return new Student(null, name, email, phone, birthDate, passwordHash, schoolId, classroomId,
                StudentStatus.PENDING_ONBOARDING, notificationOptIn);
    }

    /** RF03: guarda interesses normalizados (unicos) e conclui o onboarding quando houver ao menos um. */
    public void defineInterests(List<Interest> chosen) {
        interests.clear();
        if (chosen != null) {
            chosen.stream()
                    .filter(interest -> interests.stream().noneMatch(existing -> existing.code() == interest.code()))
                    .forEach(interests::add);
        }
        if (interests.isEmpty()) {
            interests.add(Interest.of(DEFAULT_INTEREST, 1));
        }
        if (status == StudentStatus.PENDING_ONBOARDING) {
            status = StudentStatus.ACTIVE;
        }
    }

    public void updateNotificationPreferences(boolean whatsAppOptIn) {
        this.notificationOptIn = whatsAppOptIn;
    }

    public void updateContact(String phone) {
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
    }

    public void suspend() {
        this.status = StudentStatus.SUSPENDED;
    }

    public void anonymize() {
        this.name = "Aluno Anonimizado";
        this.email = "anonimo+" + id + "@educa-mais.local";
        this.phone = null;
        this.birthDate = null;
        this.status = StudentStatus.ANONYMIZED;
        this.notificationOptIn = false;
    }

    /** RF14: interesse de maior prioridade usado pelo PromptFactory. */
    public Optional<InterestCode> primaryInterest() {
        return interests.stream()
                .max(Comparator.comparingInt(Interest::priority))
                .map(Interest::code);
    }

    public boolean isActive() {
        return status == StudentStatus.ACTIVE;
    }

    public String firstName() {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UUID schoolId() {
        return schoolId;
    }

    public void assignClassroom(UUID classroomId) {
        this.classroomId = classroomId;
    }

    public UUID classroomId() {
        return classroomId;
    }

    public StudentStatus status() {
        return status;
    }

    public boolean notificationOptIn() {
        return notificationOptIn;
    }

    public List<Interest> interests() {
        return List.copyOf(interests);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obrigatorio");
        }
        return value.trim();
    }

    private static String normalizeEmail(String email) {
        String normalized = requireText(email, "E-mail").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("E-mail invalido");
        }
        return normalized;
    }
}
