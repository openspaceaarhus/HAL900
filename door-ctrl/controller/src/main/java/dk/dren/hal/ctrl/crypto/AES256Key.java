package dk.dren.hal.ctrl.crypto;

import lombok.Getter;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AES256Key {

    @Getter(lazy = true)
    private static final KeyGenerator keyGen = createKeyGenerator();
    public static final int KEY_LENGTH_BYTES = 32;

    private static KeyGenerator createKeyGenerator() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(KEY_LENGTH_BYTES*8);
            return kg;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Java is broken", e);
        }
    }

    public static SecretKey create() {
        SecretKey secretKey = getKeyGen().generateKey();
        final byte[] aesKey = secretKey.getEncoded();
        if (aesKey.length != KEY_LENGTH_BYTES) {
            throw new RuntimeException("Unexpected key size: "+aesKey.length);
        }

        return secretKey;
    }

    public static String store(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    public static SecretKey load(String base64Encoded) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Encoded);
        if (decodedKey.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Found a key "+decodedKey.length+" bytes long it should be "+KEY_LENGTH_BYTES);
        }
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
