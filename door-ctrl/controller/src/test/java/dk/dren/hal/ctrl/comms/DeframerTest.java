package dk.dren.hal.ctrl.comms;

import lombok.experimental.NonFinal;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    private static byte[] stringToBytes(String s) {
        final String[] strings = s.split(", ");
        final byte[] result = new byte[strings.length];
        int i=0;
        for (String string : strings) {
            result[i++] = (byte)Integer.parseInt(string, 16);
        }
        return result;
    }

}