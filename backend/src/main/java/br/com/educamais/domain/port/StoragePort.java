package br.com.educamais.domain.port;

/** Armazenamento de artefatos (relatorios, assets de avatar). */
public interface StoragePort {

    String store(String key, byte[] content, String contentType);

    byte[] load(String key);
}
