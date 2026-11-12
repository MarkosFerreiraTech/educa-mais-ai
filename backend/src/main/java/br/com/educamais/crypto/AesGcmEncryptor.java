package br.com.educamais.crypto;

import br.com.educamais.domain.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifragem de PII com AES-256-GCM (RF37 / RNF12).
 *
 * <p>Formato serializado: {@code v1:<iv-b64>:<ciphertext+tag-b64>}.
 * IV aleatorio de 12 bytes por registro e tag de autenticacao de 128 bits,
 * conforme NIST SP 800-38D. A chave vem de AWS KMS/Secrets Manager em producao
 * e de variavel de ambiente em desenvolvimento (nunca versionada).</p>
 */
public class AesGcmEncryptor {

    public static final String PREFIX = "v1";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmEncryptor(byte[] rawKey) {
        if (rawKey == null || rawKey.length != 32) {
            throw new IllegalArgumentException("A chave AES-256 deve ter exatamente 32 bytes");
        }
        this.key = new SecretKeySpec(rawKey, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getEncoder();
            return "%s:%s:%s".formatted(PREFIX, encoder.encodeToString(iv), encoder.encodeToString(cipherText));
        } catch (Exception exception) {
            throw new BusinessRuleException("ENCRYPTION_FAILURE", "Falha ao cifrar dado sensivel");
        }
    }

    public String decrypt(String payload) {
        if (payload == null) {
            return null;
        }
        if (!payload.startsWith(PREFIX + ":")) {
            throw new BusinessRuleException("DECRYPTION_INVALID_PAYLOAD", "Payload cifrado invalido");
        }
        try {
            String[] parts = payload.split(":");
            if (parts.length != 3) {
                throw new BusinessRuleException("DECRYPTION_INVALID_PAYLOAD", "Payload cifrado invalido");
            }
            Base64.Decoder decoder = Base64.getDecoder();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, decoder.decode(parts[1])));
            byte[] plain = cipher.doFinal(decoder.decode(parts[2]));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (BusinessRuleException businessRuleException) {
            throw businessRuleException;
        } catch (Exception exception) {
            throw new BusinessRuleException("DECRYPTION_FAILURE", "Falha ao decifrar dado sensivel");
        }
    }

    public String blindIndex(String value) {
        if (value == null) {
            return null;
        }
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(key);
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(value.toLowerCase().trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessRuleException("BLIND_INDEX_FAILURE", "Falha ao gerar indice cego");
        }
    }
}
