package br.com.educamais.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import br.com.educamais.crypto.EncryptedStringConverter;
import java.util.UUID;

@Entity
@Table(name = "diagnostic_questions")
public class QuestionEntity {

    @Id
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "prompt", nullable = false, length = 1000)
    private String prompt;

    @Column(name = "question_type", nullable = false, length = 24)
    private String type;

    @Column(name = "options", length = 2000)
    private String options;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "correct_answer_enc", length = 512)
    private String correctAnswer;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    @Column(name = "difficulty", nullable = false)
    private int difficulty;

    public QuestionEntity() {
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

    public UUID getStepId() {
        return stepId;
    }

    public void setStepId(UUID stepId) {
        this.stepId = stepId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
