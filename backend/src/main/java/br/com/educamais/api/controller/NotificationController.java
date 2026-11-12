package br.com.educamais.api.controller;

import br.com.educamais.api.dto.NotificationResponse;
import br.com.educamais.api.mapper.ApiMapper;
import br.com.educamais.security.AuthenticatedPrincipal;
import br.com.educamais.service.NotificationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Central de notificacoes do aluno (RF23, RF26). */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return notificationService.forStudent(principal.subjectId()).stream()
                .map(ApiMapper::toNotification)
                .toList();
    }
}
