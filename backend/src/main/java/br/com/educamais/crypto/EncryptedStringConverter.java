package br.com.educamais.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Cifra automaticamente campos String sensiveis (RF37). */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : CryptoSupport.encryptor().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CryptoSupport.encryptor().decrypt(dbData);
    }
}
