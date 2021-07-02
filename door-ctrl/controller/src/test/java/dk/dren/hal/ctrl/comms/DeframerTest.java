package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.PollResponse;
import dk.dren.hal.ctrl.crypto.AES256Key;
import dk.dren.hal.ctrl.events.DeviceEvent;
import dk.dren.hal.ctrl.events.PowerUpEvent;
import lombok.experimental.NonFinal;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

public class DeframerTest {

    private Frame gotFrame;

    @Test
    public void deframeAck() {

        final byte[] bytes = stringToBytes("f0, ff, 00, 03, 00, 2e, 8a, 6c, d4, f1");

        CRC32 crc32 = new CRC32();
        for (int i=1; i< bytes.length-5; i++) {
            crc32.update(bytes[i]);
        }
        final long crc32value = crc32.getValue();
        final long expected = 0xd46c8a2eL;
        //System.out.println(String.format("Calculated crc: 0x%08x", crc32value));
        //System.out.println(String.format("Expected crc:   0x%08x", expected));
        Assert.assertEquals(expected, crc32value);


        final Deframer deframer = new Deframer(frame -> {
            gotFrame = frame;
        });
        for (byte b : bytes) {
            deframer.addByte(b);
        }

        Assertions.assertNotNull(gotFrame);
        Assertions.assertEquals("Frame: src:ff, tgt:00, type:03, payload:Empty", gotFrame.toString());
    }

    @Test
    public void deframePollResponse() {
        final byte[] bytes = stringToBytes("f0 01 00 04 21 dc 6e 37 9b cd e6 73 b9 dc 6e 37 9b cd cd e6 73 03 bd 8c 81 38 59 8f d9 3a a7 9c 2e 5f c0 08 a7 8d 75 90 b9 30 f1");
        final Deframer deframer = new Deframer(frame -> {
            gotFrame = frame;
        });
        for (byte b : bytes) {
            deframer.addByte(b);
        }

        Assertions.assertNotNull(gotFrame);

        final SecretKey aesKey = AES256Key.load("sLXeseDmXDzRGMtsAUqWfNlcHrnNFgUq+HC89UX/nrU=");
        final PollResponse pr = PollResponse.from(gotFrame, 0x47, aesKey);

        final List<DeviceEvent> events = pr.getEvents();
        Assertions.assertEquals(1, events.size());

        final DeviceEvent deviceEvent = events.get(0);
        Assertions.assertEquals(1, deviceEvent.getDeviceId());
        Assertions.assertEquals(0, deviceEvent.getType());
        Assertions.assertEquals(0, deviceEvent.getEventNumber());
        Assertions.assertInstanceOf(PowerUpEvent.class, deviceEvent);
    }


    public static byte[] stringToBytes(String s) {
        final String[] strings = s.split("[^0-9a-f]+");
        final byte[] result = new byte[strings.length];
        int i=0;
        for (String string : strings) {
            if (string.length() == 2) {
                result[i++] = (byte) Integer.parseInt(string, 16);
            }
        }
        return result;
    }

    @Test
    public void deframeManyEvents() {
        byte[] bytes = stringToBytes(
                "0000000 f0 01 00 04 e1 c6 63 b1 d8 6c 36 1b 8d c6 63 b1\n" +
                "0000020 d8 6c 36 1b 8d c8 57 55 9e 32 54 f2 77 0e da 62\n" +
                "0000040 da 28 4e ef f5 82 6b 43 00 14 30 10 29 fe 8d 9e\n" +
                "0000060 6f 79 42 32 65 de eb 5a a5 aa 1b 62 ff 41 57 d3\n" +
                "0000100 82 90 ad e0 c9 b0 62 80 ac 16 43 70 d0 e9 f1 a9\n" +
                "0000120 a5 e7 66 d4 e1 c7 9d 2f b0 36 92 d0 68 da 22 00\n" +
                "0000140 08 ce 38 4f 52 e9 df 0e 15 2b 1e 75 27 e5 83 ed\n" +
                "0000160 bc 8b a6 30 31 b1 71 7b c6 21 6b 9f b8 8c 3f a5\n" +
                "0000200 e8 9c d6 b6 b9 67 80 58 30 74 dd 19 4f 52 72 de\n" +
                "0000220 fc 01 a5 4b 55 0e 4a 3b 1b 13 be f9 90 38 b8 0c\n" +
                "0000240 e9 99 d7 78 96 49 24 d7 b4 e2 b8 aa 84 67 09 73\n" +
                "0000260 84 07 5f 85 8e e5 43 57 83 56 f7 c2 04 08 22 94\n" +
                "0000300 a0 75 ec c5 4d 9c 41 87 de c2 5f c0 8b fe 95 97\n" +
                "0000320 d9 37 05 22 2c f0 b3 a6 a5 e3 72 7e a0 b3 07 6b\n" +
                "0000340 95 4c 07 25 49 bb 18 ba b9 31 f1\n" +
                "0000353\n");

        final Deframer deframer = new Deframer(frame -> gotFrame = frame);
        for (byte b : bytes) {
            deframer.addByte(b);
        }

        Assertions.assertNotNull(gotFrame);

        final SecretKey aesKey = AES256Key.load("sLXeseDmXDzRGMtsAUqWfNlcHrnNFgUq+HC89UX/nrU=");
        final PollResponse pr = PollResponse.from(gotFrame, 0x47, aesKey);

        final List<DeviceEvent> events = pr.getEvents();
        Assertions.assertEquals(50, events.size());
    }

}