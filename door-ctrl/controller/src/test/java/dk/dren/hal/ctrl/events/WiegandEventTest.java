package dk.dren.hal.ctrl.events;

import dk.dren.hal.ctrl.comms.DeframerTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WiegandEventTest {

    @Test
    public void printOld() {
        long[] known26bit = new long[]{2426005, 6128355, 2206954};

        for (long k26 : known26bit) {
            print("HAL",k26);
        }
    }

//    @Test
    public void testSwap() {
        long wg34 = 0x24a821280L;
        System.out.println(Long.toBinaryString(wg34));

        byte[] data = new byte[]{34, (byte) 0x80, (byte) 0x12, (byte) 0x82, 0x4a, 0x02, 0x00, 0x00};
        final long unpacked = WiegandEvent.unpackBytes(data);
        System.out.println(Long.toBinaryString(unpacked));
    }

    @Test
    public void unpackKey() {
        byte[] data1 = new byte[]{4, (byte)0x0a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        final long unpacked1 = WiegandEvent.unpackBytes(data1);
        print("Key",unpacked1);
    }

    @Test
    public void unpackOldBlue() {
        final long unpacked = WiegandEvent.unpackBytes(DeframerTest.stringToBytes("22 80 2e c1 71 02 00 00"));
        print("OB",unpacked);
        //Assertions.assertEquals();

    }

    @Test
    public void unpackOldCard() {
        final long unpacked = WiegandEvent.unpackBytes(DeframerTest.stringToBytes("22 80 12 82 4a 02 00 00"));
        print("OC",unpacked);
        //Assertions.assertEquals();

    }

    @Test
    public void unpackNewCard() {
        final long unpacked = WiegandEvent.unpackBytes(DeframerTest.stringToBytes("22 0b 69 f9 61 01 00 00"));
        print("NC",unpacked);
        //Assertions.assertEquals();

    }

    private void print(String tag, long unpacked) {
        String bin = Long.toBinaryString(unpacked);
        while (bin.length() < 34) {
            bin = "0" + bin;
        }
        System.out.println(tag+"\t"+ bin + String.format(" %d 0x%x", unpacked, unpacked));
    }

    @Test
    public void unpackNFC() {
        final long unpacked = WiegandEvent.unpackBytes(DeframerTest.stringToBytes("22 82 01 81 00 03 00 00"));
        print("NFC",unpacked);
        //Assertions.assertEquals();

    }

    @Test
    public void conv() {
        print("CNV", 2152335658L);
        //Assertions.assertEquals();

    }




}