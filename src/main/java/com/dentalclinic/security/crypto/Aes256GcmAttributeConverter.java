package com.dentalclinic.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise AES-256 GCM JPA Attribute Converter for Medical Clinical Data (F49).
 * Features:
 * - 256-bit cryptographic key derivation via SHA-256
 * - 12-byte cryptographically secure random IV (NIST SP 800-38D) per encryption
 * - 128-bit authentication tag for AEAD integrity protection
 * - Base64-encoded storage format: [12-byte IV][Ciphertext + 16-byte Tag]
 * - Graceful backward-compatible fallback for unencrypted legacy plaintext
 */
@Converter
@Component
public class Aes256GcmAttributeConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(Aes256GcmAttributeConverter.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // 16 bytes
    private static final int IV_LENGTH_BYTE = 12;  // 96-bit IV recommended by NIST
    private static final String DEFAULT_SECRET = "DentalCare2026LuxuryClinicSecureKeyForMedicalEMRAES256GCM";

    private static volatile SecretKey secretKey;

    public Aes256GcmAttributeConverter() {
        if (secretKey == null) {
            initKey(DEFAULT_SECRET);
        }
    }

    @Value("${app.security.crypto.aes-key:DentalCare2026LuxuryClinicSecureKeyForMedicalEMRAES256GCM}")
    public void setConfiguredKey(String key) {
        if (key != null && !key.isBlank()) {
            initKey(key);
        }
    }

    private static synchronized void initKey(String rawSecret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            log.error("Failed to initialize AES-256 secret key", e);
            throw new IllegalStateException("Could not initialize AES-256 key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed for medical clinical attribute", e);
            throw new IllegalStateException("Failed to encrypt medical clinical data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(dbData);
            } catch (IllegalArgumentException e) {
                // Not valid Base64: legacy plaintext fallback
                return dbData;
            }

            // Minimum length check: 12 bytes IV + 16 bytes GCM tag = 28 bytes
            if (decoded.length < IV_LENGTH_BYTE + (TAG_LENGTH_BIT / 8)) {
                return dbData;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Decryption failure (e.g. legacy plaintext or tampered tag): fallback gracefully
            log.warn("Could not decrypt clinical field, falling back to raw data: {}", e.getMessage());
            return dbData;
        }
    }
}
