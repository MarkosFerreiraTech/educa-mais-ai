package br.com.educamais.api.controller;

import br.com.educamais.api.dto.GamificationResponse;
import br.com.educamais.api.dto.ProfileResponse;
import br.com.educamais.api.dto.UpdatePreferencesRequest;
import br.com.educamais.api.mapper.ApiMapper;
import br.com.educamais.domain.model.Student;
import br.com.educamais.security.AuthenticatedPrincipal;
import br.com.educamais.service.StudentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil e gamificacao do aluno autenticado (RF05, RF32). */
@RestController
@RequestMapping("/api/v1/students/me")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ProfileResponse profile(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiMapper.toProfile(studentService.get(principal.subjectId()));
    }

    @GetMapping("/gamification")
    public GamificationResponse gamification(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiMapper.toGamification(studentService.profileOf(principal.subjectId()));
    }

    @PatchMapping("/preferences")
    public ProfileResponse updatePreferences(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                             @RequestBody UpdatePreferencesRequest request) {
        Student student = studentService.updatePreferences(principal.subjectId(), request.whatsAppOptIn(),
                request.phone());
        return ApiMapper.toProfile(student);
    }
}
