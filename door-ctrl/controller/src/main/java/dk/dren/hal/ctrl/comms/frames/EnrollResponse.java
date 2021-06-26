package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.BusDevice;
import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Frame;
import dk.dren.hal.ctrl.crypto.AES256Key;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;

@RequiredArgsConstructor
@Getter
public class EnrollResponse {
    private static final int TYPE = 0x02;
    public static final int PAYLOAD_SIZE = 4 + 1 + 32;
    private final Frame frame;
    private final BusDevice busDevice;

    static public EnrollResponse create(EnrollRequest request, int firstFreeNodeId) {
        final SecretKey secretKey = AES256Key.create();

        final BusDevice busDevice = new BusDevice(firstFreeNodeId, secretKey);

        final byte[] aesKeyBytes = secretKey.getEncoded();
        final int payloadSize = request.getTemporaryId().size() + 1 + aesKeyBytes.length;
        if (payloadSize != PAYLOAD_SIZE) {
            throw new RuntimeException("Error "+payloadSize+" should be "+PAYLOAD_SIZE);
        }
        final ByteBuffer payload = new ByteBuffer(payloadSize);

        payload.add(request.getTemporaryId().toArray());
        payload.add((byte)firstFreeNodeId);
        payload.add(aesKeyBytes);

        final Frame frame = new Frame((byte) 0x00, (byte)0xff, (byte)TYPE, payload);

        return new EnrollResponse(frame, busDevice);

    }
}
