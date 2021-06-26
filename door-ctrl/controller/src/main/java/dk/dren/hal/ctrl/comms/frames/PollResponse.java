package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Deframer;
import dk.dren.hal.ctrl.comms.Frame;
import dk.dren.hal.ctrl.events.BinaryEventParser;
import dk.dren.hal.ctrl.events.DeviceEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.AlgorithmParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

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

        List<DeviceEvent> events = new ArrayList<>();
        int pos = 0;
        while (pos < unpaddedSize) {
            final byte type = plainText.get(pos);
            final byte counter = plainText.get(pos + 1);
            final byte size = plainText.get(pos + 2);
            DeviceEvent e = BinaryEventParser.parse(deviceId, type, counter, plainText.toArray(pos+3, size));
            events.add(e);
            pos += 3+size;
        }

        return new PollResponse(frame, events);
    }
}
