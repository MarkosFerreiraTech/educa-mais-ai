package br.com.educamais.domain.port;

/** Porta de hash de senha (BCrypt no adapter de seguranca). */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
