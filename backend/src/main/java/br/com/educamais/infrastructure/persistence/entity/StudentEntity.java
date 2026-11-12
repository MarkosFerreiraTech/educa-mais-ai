package br.com.educamais.infrastructure.persistence.entity;

import br.com.educamais.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;

/** Persistencia de aluno; campos sensiveis cifrados em AES-256-GCM (RF37). */
@Entity
@Table(name = "students")
public class StudentEntity {

    @Id
    private UUID id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "name_enc", nullable = false, length = 512)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email_enc", nullable = false, length = 512)
    private String email;

    @Column(name = "email_hash", nullable = false, unique = true, length = 128)
    private String emailHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_enc", length = 512)
    private String phone;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "birth_date_enc", length = 512)
    private String birthDate;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "notification_opt_in", nullable = false)
    private boolean notificationOptIn;

    @Column(name = "interests", length = 512)
    private String interests;

    @Version
    @Column(name = "version")
    private Long version;

    protected StudentEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate birthDateAsLocalDate() {
        return birthDate == null ? null : LocalDate.parse(birthDate);
    }

    public void setBirthDate(LocalDate date) {
        this.birthDate = date == null ? null : date.toString();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(UUID classroomId) {
        this.classroomId = classroomId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isNotificationOptIn() {
        return notificationOptIn;
    }

    public void setNotificationOptIn(boolean notificationOptIn) {
        this.notificationOptIn = notificationOptIn;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public Long getVersion() {
        return version;
    }
}
