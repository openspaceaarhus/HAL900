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

class DeframerTest {

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
            result[i++] = (byte)Integer.parseInt(string, 16);
        }
        return result;
    }

}