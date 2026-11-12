package br.com.educamais.infrastructure.persistence.adapter;

import br.com.educamais.domain.model.TeacherReport;
import br.com.educamais.domain.port.ReportRepository;
import br.com.educamais.infrastructure.persistence.EntityMapper;
import br.com.educamais.infrastructure.persistence.jpa.ReportJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReportPersistenceAdapter implements ReportRepository {

    private final ReportJpaRepository repository;

    public ReportPersistenceAdapter(ReportJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TeacherReport save(TeacherReport report) {
        return EntityMapper.toDomain(repository.save(EntityMapper.toEntity(report)));
    }

    @Override
    public Optional<TeacherReport> findByMissionId(UUID missionId) {
        return repository.findByMissionId(missionId).map(EntityMapper::toDomain);
    }

    @Override
    public List<TeacherReport> findByStudentId(UUID studentId) {
        return repository.findByStudentId(studentId).stream().map(EntityMapper::toDomain).toList();
    }
}
