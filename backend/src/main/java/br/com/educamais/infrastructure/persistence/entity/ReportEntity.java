package br.com.educamais.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_reports")
public class ReportEntity {

    @Id
    private UUID id;

    @Column(name = "mission_id", nullable = false, unique = true)
    private UUID missionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Column(name = "identified_gaps", length = 2000)
    private String identifiedGaps;

    @Column(name = "recommendation", length = 2000)
    private String recommendation;

    @Column(name = "success_rate", nullable = false)
    private int successRate;

    @Column(name = "delivery_status", nullable = false, length = 24)
    private String deliveryStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReportEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMissionId() {
        return missionId;
    }

    public void setMissionId(UUID missionId) {
        this.missionId = missionId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getIdentifiedGaps() {
        return identifiedGaps;
    }

    public void setIdentifiedGaps(String identifiedGaps) {
        this.identifiedGaps = identifiedGaps;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public int getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(int successRate) {
        this.successRate = successRate;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
