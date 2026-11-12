package br.com.educamais.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.domain.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF37, RNF12: cifragem AES-256-GCM de PII. */
class AesGcmEncryptorTest {

    private AesGcmEncryptor encryptor;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        encryptor = new AesGcmEncryptor(key);
    }

    @Test
    @DisplayName("Cifra e decifra preservando o valor")
    void roundTrip() {
        String phone = "+5581999998888";
        String encrypted = encryptor.encrypt(phone);

        assertThat(encrypted).startsWith("v1:").isNotEqualTo(phone);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(phone);
    }

    @Test
    @DisplayName("Mesmo texto gera cifras diferentes (IV aleatorio)")
    void ivAleatorio() {
        String first = encryptor.encrypt("ana@escola.br");
        String second = encryptor.encrypt("ana@escola.br");

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo(encryptor.decrypt(second));
    }

    @Test
    @DisplayName("Texto adulterado falha na autenticacao GCM")
    void adulteracao() {
        String encrypted = encryptor.encrypt("dado sensivel");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Payload sem prefixo de versao e rejeitado")
    void payloadInvalido() {
        assertThatThrownBy(() -> encryptor.decrypt("texto-claro")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Nulos passam adiante sem cifragem")
    void nulos() {
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("Indice cego e deterministico e nao expoe o valor")
    void indiceCego() {
        String first = encryptor.blindIndex("Ana@Escola.br");
        String second = encryptor.blindIndex("ana@escola.br");

        assertThat(first).isEqualTo(second).doesNotContain("ana");
    }

    @Test
    @DisplayName("Chave de tamanho invalido e rejeitada")
    void chaveInvalida() {
        assertThatThrownBy(() -> new AesGcmEncryptor("chave-curta".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
