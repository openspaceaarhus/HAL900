package dk.dren.hal.ctrl.crypto;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.zip.CRC32;

@Getter
public class PayloadEncryptor {

    public static final SecureRandom secureRandom = new SecureRandom();
    private final ByteBuffer encrypted;

    @SneakyThrows
    public PayloadEncryptor(ByteBuffer payload, SecretKey aesKey) {
        final int unpaddedSize = payload.size();
        int paddedSize = unpaddedSize+4;
        while ((paddedSize & 15) != 0) {
            paddedSize++;
        }

        final ByteBuffer plainTextPadded = new ByteBuffer(paddedSize);
        plainTextPadded.add(payload.toArray());
        addCRC(plainTextPadded, unpaddedSize);
        while (!plainTextPadded.isFull()) {
            plainTextPadded.add((byte)0);
        }

        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);

        final Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, aesKey, ivParams);

        final byte[] encryptedBytes = aes.doFinal(plainTextPadded.toArray());

        encrypted = new ByteBuffer(paddedSize + 16 + 1);
        encrypted.add(iv);
        encrypted.add((byte)unpaddedSize);
        encrypted.add(encryptedBytes);
    }


    private static void addCRC(ByteBuffer payload, int actualPayloadSize) {
        CRC32 crc32 = new CRC32();
        for (int i = 0; i < actualPayloadSize; i++) {
            crc32.update(payload.get(i));
        }
        final long crc32value = crc32.getValue();

        //log.info(()->String.format("crc: %x", crc32value));
        payload.add((byte)(crc32value & 0xff));
        payload.add((byte)((crc32value>>8) & 0xff));
        payload.add((byte)((crc32value>>16) & 0xff));
        payload.add((byte)((crc32value>>24) & 0xff));
    }
}
