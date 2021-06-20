package dk.dren.hal.ctrl.comms;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class FrameTest {

    @Test
    public void testcrcEmpty() {
        CRC32 crc32 = new CRC32();
        final long crc32value = crc32.getValue();

        Assert.assertEquals(0, crc32value);
    }

    @Test
    public void testcrcZero() {
        CRC32 crc32 = new CRC32();
        crc32.update(0);
        final long crc32value = crc32.getValue();
        System.out.println(String.format("%08x", crc32value));
        Assert.assertEquals(0xd202ef8d, crc32value);
    }

}