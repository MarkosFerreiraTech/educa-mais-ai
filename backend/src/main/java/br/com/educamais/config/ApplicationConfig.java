package br.com.educamais.config;

import br.com.educamais.crypto.AesGcmEncryptor;
import br.com.educamais.crypto.CryptoSupport;
import br.com.educamais.domain.port.ClockPort;
import br.com.educamais.domain.port.PasswordHasherPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans de infraestrutura e wiring de dependencias (DIP/DI).
 * A chave AES vem de variavel de ambiente (dev) ou KMS/Secrets Manager (producao).
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public AesGcmEncryptor aesGcmEncryptor(SecurityProperties properties) {
        String configured = properties.getEncryptionKey();
        byte[] key;
        if (configured == null || configured.isBlank()) {
            // Modo desenvolvimento: chave derivada deterministica. Em producao o startup falha
            // porque a variavel ENCRYPTION_KEY e obrigatoria (ver application-prod.yml).
            key = deriveDevelopmentKey();
        } else {
            key = decodeKey(configured);
        }
        AesGcmEncryptor encryptor = new AesGcmEncryptor(key);
        CryptoSupport.register(encryptor);
        return encryptor;
    }

    @Bean
    public ClockPort clockPort() {
        return new ClockPort() {
            @Override
            public Instant now() {
                return Instant.now();
            }

            @Override
            public LocalDate today() {
                return LocalDate.now();
            }
        };
    }

    @Bean
    public PasswordHasherPort passwordHasherPort(PasswordEncoder passwordEncoder) {
        return new PasswordHasherPort() {
            @Override
            public String hash(String rawPassword) {
                return passwordEncoder.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String hashedPassword) {
                return hashedPassword != null && passwordEncoder.matches(rawPassword, hashedPassword);
            }
        };
    }

    private static byte[] deriveDevelopmentKey() {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest("educa-mais-dev-only-key".getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao derivar chave de desenvolvimento", exception);
        }
    }

    private static byte[] decodeKey(String configured) {
        if (configured.length() == 64 && configured.matches("[0-9a-fA-F]+")) {
            byte[] decoded = new byte[32];
            for (int index = 0; index < 32; index++) {
                decoded[index] = (byte) Integer.parseInt(configured.substring(index * 2, index * 2 + 2), 16);
            }
            return decoded;
        }
        byte[] raw = configured.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception exception) {
            throw new IllegalStateException("Chave de cifragem invalida", exception);
        }
    }
}
