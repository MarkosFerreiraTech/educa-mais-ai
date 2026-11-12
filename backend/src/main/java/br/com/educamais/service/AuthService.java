package br.com.educamais.service;

import br.com.educamais.domain.enums.Role;
import br.com.educamais.domain.exception.BusinessRuleException;
import br.com.educamais.domain.exception.DuplicateResourceException;
import br.com.educamais.domain.model.Interest;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.PasswordHasherPort;
import br.com.educamais.domain.port.StudentRepository;
import br.com.educamais.security.JwtService;
import br.com.educamais.security.LoginAttemptGuard;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro, login e renovacao de sessao (RF01, RF05 / RNF06, RNF14). */
@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final PasswordHasherPort passwordHasher;
    private final JwtService jwtService;
    private final LoginAttemptGuard loginAttemptGuard;

    public AuthService(StudentRepository studentRepository, PasswordHasherPort passwordHasher,
                       JwtService jwtService, LoginAttemptGuard loginAttemptGuard) {
        this.studentRepository = studentRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    @Transactional
    public AuthResult register(String name, String email, String phone, LocalDate birthDate, String rawPassword,
                               List<Interest> interests, UUID schoolId, UUID classroomId, boolean whatsAppOptIn) {
        if (studentRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("EMAIL_ALREADY_REGISTERED",
                    "E-mail ja cadastrado: " + email);
        }
        Student student = Student.register(name, email, phone, birthDate, passwordHasher.hash(rawPassword),
                schoolId, classroomId, whatsAppOptIn);
        student.defineInterests(interests);
        Student saved = studentRepository.save(student);
        return issueTokens(saved);
    }

    /** Atualiza interesses apos o cadastro, mantendo o onboarding coerente (RF03). */
    @Transactional
    @org.springframework.transaction.annotation.Transactional
    public Student updateInterests(UUID studentId, List<Interest> interests) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessRuleException("RESOURCE_NOT_FOUND", "Aluno nao encontrado"));
        student.defineInterests(interests);
        return studentRepository.save(student);
    }

    public AuthResult login(String email, String rawPassword) {
        loginAttemptGuard.checkAllowed(email);
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CREDENTIALS", "Credenciais invalidas"));
        if (!passwordHasher.matches(rawPassword, student.passwordHash())) {
            loginAttemptGuard.registerFailure(email);
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Credenciais invalidas");
        }
        if (!student.isActive() && student.status() != br.com.educamais.domain.enums.StudentStatus.PENDING_ONBOARDING) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE", "Conta inativa");
        }
        loginAttemptGuard.registerSuccess(email);
        return issueTokens(student);
    }

    /** Troca o codigo do Authelia (OIDC) por um JWT interno (RF09 / RNF09). */
    public AuthResult issueForExternalIdentity(UUID subjectId, String email, Role role, UUID schoolId) {
        String accessToken = jwtService.generateAccessToken(subjectId, email, role, schoolId);
        return new AuthResult(accessToken, jwtService.generateRefreshToken(subjectId), "Bearer",
                jwtService.accessTokenTtlSeconds(), role.name());
    }

    public AuthResult refresh(String refreshToken) {
        try {
            var claims = jwtService.parseClaims(refreshToken);
            if (!"refresh".equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                throw new BusinessRuleException("INVALID_REFRESH_TOKEN", "Token de refresh invalido");
            }
            UUID studentId = UUID.fromString(claims.getSubject());
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new BusinessRuleException("INVALID_REFRESH_TOKEN", "Aluno nao encontrado"));
            return issueTokens(student);
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("INVALID_REFRESH_TOKEN", "Refresh token invalido ou expirado");
        }
    }

    private AuthResult issueTokens(Student student) {
        String accessToken = jwtService.generateAccessToken(student.id(), student.email(), Role.ALUNO,
                student.schoolId());
        return new AuthResult(accessToken, jwtService.generateRefreshToken(student.id()), "Bearer", 3600,
                Role.ALUNO.name());
    }


    public record AuthResult(String accessToken, String refreshToken, String tokenType, long expiresIn,
                             String role) {
    }
}
