package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.DiagnosticAttempt;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.port.DiagnosticRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.jpa.AttemptJpaRepository;
import br.com.educamais.infrastructure.persistence.jpa.QuestionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticPersistenceAdapter implements DiagnosticRepository {

    private final QuestionJpaRepository questionRepository;
    private final AttemptJpaRepository attemptRepository;

    public DiagnosticPersistenceAdapter(QuestionJpaRepository questionRepository,
                                        AttemptJpaRepository attemptRepository) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    @Override
    public DiagnosticQuestion saveQuestion(DiagnosticQuestion question) {
        return EntityMapper.toDomain(questionRepository.save(EntityMapper.toEntity(question)));
    }

    @Override
    public Optional<DiagnosticQuestion> findQuestionById(UUID id) {
        return questionRepository.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public List<DiagnosticQuestion> findQuestionsByMissionId(UUID missionId) {
        return questionRepository.findByMissionId(missionId).stream().map(EntityMapper::toDomain).toList();
    }

    @Override
    public DiagnosticAttempt saveAttempt(DiagnosticAttempt attempt) {
        return EntityMapper.toDomain(attemptRepository.save(EntityMapper.toEntity(attempt)));
    }

    @Override
    public List<DiagnosticAttempt> findAttemptsByStudentId(UUID studentId) {
        return attemptRepository.findByStudentId(studentId).stream().map(EntityMapper::toDomain).toList();
    }
}
