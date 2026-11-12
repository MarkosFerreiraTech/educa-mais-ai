package br.com.educamais.api.controller;

import br.com.educamais.api.dto.AnswerRequest;
import br.com.educamais.api.dto.AnswerResponse;
import br.com.educamais.api.dto.MissionResponse;
import br.com.educamais.api.mapper.ApiMapper;
import br.com.educamais.security.AuthenticatedPrincipal;
import br.com.educamais.service.MissionRecoveryFacade;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Jornada de recuperacao do aluno (RF27, RF28 / UC05). */
@RestController
@RequestMapping("/api/v1/missions")
public class MissionController {

    private final MissionRecoveryFacade facade;

    public MissionController(MissionRecoveryFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<MissionResponse> list(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return facade.studentMissions(principal.subjectId()).stream()
                .map(mission -> ApiMapper.toMission(mission, null))
                .toList();
    }

    @GetMapping("/{missionId}")
    public MissionResponse detail(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                  @PathVariable UUID missionId) {
        return ApiMapper.toJourney(facade.current(missionId, principal.subjectId()));
    }

    @PostMapping("/{missionId}/start")
    public MissionResponse start(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                 @PathVariable UUID missionId) {
        return ApiMapper.toJourney(facade.start(missionId, principal.subjectId()));
    }

    @PostMapping("/{missionId}/answers")
    public AnswerResponse answer(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                 @PathVariable UUID missionId, @Valid @RequestBody AnswerRequest request) {
        MissionRecoveryFacade.MissionJourney journey = facade.answer(missionId, principal.subjectId(),
                request.questionId(), request.answer());
        String feedback = journey.mission().isFinished()
                ? journey.feedback() + " Missao concluida: +50 XP e +1 diamante!"
                : journey.feedback();
        return new AnswerResponse(journey.classification(), feedback, ApiMapper.toJourney(journey));
    }
}
