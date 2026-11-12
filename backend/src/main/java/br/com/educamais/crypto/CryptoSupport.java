package br.com.educamais.crypto;

/**
 * Ponte entre o contexto Spring e os {@code AttributeConverter} JPA,
 * que sao instanciados pelo provedor de persistencia (sem injecao de dependencia).
 */
public final class CryptoSupport {

    private static AesGcmEncryptor encryptor;

    private CryptoSupport() {
    }

    public static void register(AesGcmEncryptor instance) {
        CryptoSupport.encryptor = instance;
    }

    public static AesGcmEncryptor encryptor() {
        if (encryptor == null) {
            throw new IllegalStateException("Cifrador nao inicializado: verifique a configuracao de seguranca");
        }
        return encryptor;
    }
}
