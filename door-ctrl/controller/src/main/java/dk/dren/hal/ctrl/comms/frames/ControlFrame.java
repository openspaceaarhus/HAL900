package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Frame;
import dk.dren.hal.ctrl.crypto.PayloadEncryptor;
import lombok.extern.java.Log;

import javax.crypto.SecretKey;

@Log
public class ControlFrame {
    public static final int TYPE = 0x05;
    public static final int ACTUAL_PAYLOAD_SIZE = 1 + 4 + 1 + 1 + 1;
    public static final int PAYLOAD_SIZE_WITH_CRC = ACTUAL_PAYLOAD_SIZE + 4;

    public static Frame create(int targetId, int lastEventSeen, SecretKey aesKey, byte[] controlToken, int state0, int timeout, int state1) {

        final ByteBuffer payload = new ByteBuffer(PAYLOAD_SIZE_WITH_CRC);
        payload.add((byte)lastEventSeen);
        if (controlToken == null) {
            payload.add((byte)0);
            payload.add((byte)0);
            payload.add((byte)0);
            payload.add((byte)0);
        } else {
            log.info(String.format("Using token %x %x %x %x to set %x", controlToken[3], controlToken[2], controlToken[1], controlToken[0], state0));
            payload.add(controlToken);
        }
        payload.add((byte)state0);
        payload.add((byte)timeout);
        payload.add((byte)state1);

        final PayloadEncryptor payloadEncryptor = new PayloadEncryptor(payload, aesKey);
        return new Frame((byte)0x00, (byte)targetId, (byte)TYPE, payloadEncryptor.getEncrypted());
    }
}
