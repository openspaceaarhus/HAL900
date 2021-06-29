package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Frame;
import dk.dren.hal.ctrl.crypto.PayloadDecryptor;
import dk.dren.hal.ctrl.events.BinaryEventParser;
import dk.dren.hal.ctrl.events.DeviceEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class PollResponse {
    public static final int TYPE = 0x04;
    private final Frame frame;
    private final List<DeviceEvent> events;

    @SneakyThrows
    public static PollResponse from(Frame frame, int deviceId, SecretKey aesKey) {

        final ByteBuffer payload = frame.getPayload();

        // 16 bytes of IV
        // 1 byte of actual payload size sans padding
        // data
        PayloadDecryptor pd = new PayloadDecryptor(payload, aesKey);
/*
        final Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(payload.toArray(0, 16)));
        final byte unpaddedSize = payload.get(16);
        final ByteBuffer plainText = new ByteBuffer(aes.doFinal(payload.toArray(17, payload.size() - 17)));

        final long crc32FromText = Deframer.read32bitLittleEndian(plainText, unpaddedSize);
        CRC32 crc32 = new CRC32();
        for (int i = 0; i < unpaddedSize; i++) {
            crc32.update(plainText.get(i));
        }
        if (crc32FromText != crc32.getValue()) {
            throw new IllegalArgumentException(String.format("Bad crc: %08x vs %08x",
                    crc32FromText, crc32.getValue()));
        }
*/
        List<DeviceEvent> events = new ArrayList<>();
        int pos = 0;
        int unpaddedSize = pd.getUnpaddedSize();
        final ByteBuffer plainText = pd.getPlainText();
        while (pos < unpaddedSize) {
            final int type    = 0xff & plainText.get(pos);
            final int counter = 0xff & plainText.get(pos + 1);
            final int size    = 0xff & plainText.get(pos + 2);
            DeviceEvent e = BinaryEventParser.parse(deviceId, type, counter, plainText.toArray(pos+3, size));
            events.add(e);
            pos += 3+size;
        }

        return new PollResponse(frame, events);
    }
}
