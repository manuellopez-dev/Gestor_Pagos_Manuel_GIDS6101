package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@Slf4j
public class AesCryptoServiceImpl implements CryptoService {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] aesKeyBytes;
    private final byte[] hmacKeyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesCryptoServiceImpl(
            @Value("${gestopago.seguridad.aes-secret-key:GestoPagoSecretKey2026AES256Bit!}") String aesKey,
            @Value("${gestopago.seguridad.firma-secret-key:GestoPagoHmacSha256SignatureSecretKey!}") String firmaKey) {
        this.aesKeyBytes = derivarClave32Bytes(aesKey);
        this.hmacKeyBytes = firmaKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("Error al generar firma digital HMAC-SHA256: {}", e.getMessage());
            throw new IllegalStateException("Error al firmar digitalmente el payload", e);
        }
    }

    @Override
    public String cifrarAes(String contenido) {
        try {
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] cipherText = cipher.doFinal(contenido.getBytes(StandardCharsets.UTF_8));

            // Concatenar IV + Texto Cifrado para descifrado seguro
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Error al cifrar contenido con AES: {}", e.getMessage());
            throw new IllegalStateException("Error al cifrar la carga de datos con AES", e);
        }
    }

    @Override
    public String descifrarAes(String cifrado) {
        try {
            byte[] combined = Base64.getDecoder().decode(cifrado);
            if (combined.length < 16) {
                throw new IllegalArgumentException("Longitud de texto cifrado no válida");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(combined, 16, combined.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] original = cipher.doFinal(cipherText);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar contenido con AES: {}", e.getMessage());
            throw new IllegalStateException("Error al descifrar la carga de datos con AES", e);
        }
    }

    private byte[] derivarClave32Bytes(String key) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Error al derivar clave criptográfica", e);
        }
    }
}
