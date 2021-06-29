package dk.dren.hal.ctrl.crypto;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

class PayloadEncryptorTest {

    @Test
    public void fullCircle() {
        SecretKey key = AES256Key.create();
        final PayloadEncryptor encrypted = new PayloadEncryptor(new ByteBuffer("Hello World"), key);
        final PayloadDecryptor payloadDecryptor = new PayloadDecryptor(encrypted.getEncrypted(), key);
        Assertions.assertEquals("Hello World", payloadDecryptor.getPlainText().toString());
    }

}