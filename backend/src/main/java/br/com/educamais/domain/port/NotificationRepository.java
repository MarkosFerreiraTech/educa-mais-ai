package br.com.educamais.domain.port;

import br.com.educamais.domain.model.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    List<Notification> findByStudentId(UUID studentId);

    List<Notification> findByMissionId(UUID missionId);
}
