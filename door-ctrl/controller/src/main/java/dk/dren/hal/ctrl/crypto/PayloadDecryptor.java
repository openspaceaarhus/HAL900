package dk.dren.hal.ctrl.crypto;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Deframer;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.zip.CRC32;

/**
 * Handles decryption of an encrypted payload that contains:
 *
 * 16    IV
 * 1     unpadded size
 * n*16  AES blocks
 */
@Getter
public class PayloadDecryptor {
    private final int unpaddedSize;
    private final ByteBuffer plainText;

    @SneakyThrows
    public PayloadDecryptor(ByteBuffer payload, SecretKey aesKey) {
        final Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(payload.toArray(0, 16)));
        unpaddedSize = ((int)payload.get(16)) & 0xff;
        ByteBuffer plainTextWithCRCAndPadding = new ByteBuffer(aes.doFinal(payload.toArray(17, payload.size() - 17)));

        final long crc32FromText = Deframer.read32bitLittleEndian(plainTextWithCRCAndPadding, unpaddedSize);
        CRC32 crc32 = new CRC32();
        for (int i = 0; i < unpaddedSize; i++) {
            crc32.update(plainTextWithCRCAndPadding.get(i));
        }
        if (crc32FromText != crc32.getValue()) {
            throw new IllegalArgumentException(String.format("Bad crc: %08x vs %08x",
                    crc32FromText, crc32.getValue()));
        }

        plainText = new ByteBuffer(plainTextWithCRCAndPadding.toArray(0, unpaddedSize));
    }

}
