package dk.dren.hal.ctrl.events;

import dk.dren.hal.ctrl.comms.DeframerTest;
import org.junit.jupiter.api.Test;

class WiegandEventTest {

    @Test
    public void printOld() {
        long[] known26bit = new long[]{2426005, 6128355, 2206954};

        for (long k26 : known26bit) {
            print("HAL",k26);
        }
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




}