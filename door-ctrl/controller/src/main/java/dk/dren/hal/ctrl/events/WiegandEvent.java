package dk.dren.hal.ctrl.events;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class WiegandEvent implements DeviceEvent {
    public static final int TYPE=0x01;

    public static final int BITS_IN_KEYPRESS = 4;
    public static final int BITS_IN_RFID = 34;
    public static final long KEY_STAR = 0xa;
    public static final long KEY_HASH = 0xb;
    public static final long KEY_BELL = 0xc;

    private final int deviceId;
    private final int eventNumber;
    private final int bits;
    private final long data;
    private final byte[] rawData;

    public WiegandEvent(int deviceId, byte counter, byte[] data) {
        this.deviceId = deviceId;
        this.eventNumber = counter;
        this.bits = data[0];
        this.data = unpackBytes(data);
        this.rawData = data;
    }

    /**
     * The wiegand data is packed into bytes so that
     * @param data
     * @return
     */
    public static long unpackBytes(byte[] data) {
        long result = 0;
        byte bits = data[0];
        for (int i=1;i<8;i++) {
            int bitsFromThisByte = Math.min(bits, 8);
            result <<= bitsFromThisByte;
            result |= data[i] & ((1<<bitsFromThisByte)-1);
            bits -= bitsFromThisByte;
            if (bits == 0) {
                break;
            }
        }

        return result;
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String toData() {
        StringBuilder sb = new StringBuilder();
        sb.append(deviceId).append("\t").append(TYPE).append("\t").append(eventNumber).append("\t").append(data).append("\t");
        sb.append(String.format("Wiegand %d bits: 0x%x (%s)", bits, data, bytesToHexString(rawData)));
        return sb.toString();
    }

    private String bytesToHexString(byte[] rawData) {
        StringBuilder sb = new StringBuilder();

        String sep = "";
        for (byte rb : rawData) {
            sb.append(sep).append(String.format("%02x", rb));
            sep = " ";
        }

        return sb.toString();
    }

    public boolean isKeyPress() {
        return bits == BITS_IN_KEYPRESS;
    }

    public boolean isRFID() {
        return bits == BITS_IN_RFID;
    }
}
