package br.com.educamais.api.controller;

import br.com.educamais.api.dto.LoginRequest;
import br.com.educamais.api.dto.LoginResponse;
import br.com.educamais.api.dto.RefreshRequest;
import br.com.educamais.api.dto.RegisterRequest;
import br.com.educamais.api.mapper.ApiMapper;
import br.com.educamais.domain.enums.InterestCode;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.security.AuthenticatedPrincipal;
import br.com.educamais.service.AuthService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de identidade (RF01, RF02, RF03, RF09). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        List<Interest> interests = toInterests(request.interests());
        AuthService.AuthResult result = authService.register(request.name(), request.email(), request.phone(),
                request.birthDate(), request.password(), interests, request.schoolId(), request.classroomId(),
                request.whatsAppOptIn());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiMapper.toLoginResponse(result));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return ApiMapper.toLoginResponse(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiMapper.toLoginResponse(authService.refresh(request.refreshToken()));
    }

    @PatchMapping("/interests")
    public ResponseEntity<Void> updateInterests(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                @RequestBody List<String> interests) {
        authService.updateInterests(principal.subjectId(), toInterests(interests));
        return ResponseEntity.noContent().build();
    }

    /** Converte codigos do cliente em interesses com prioridade decrescente na ordem escolhida. */
    private static List<Interest> toInterests(List<String> codes) {
        List<Interest> mapped = new ArrayList<>();
        if (codes == null) {
            return mapped;
        }
        int priority = codes.size();
        for (String code : codes) {
            int currentPriority = priority - mapped.size();
            InterestCode.fromCode(code).ifPresent(interest -> mapped.add(Interest.of(interest, currentPriority)));
        }
        return mapped;
    }
}
