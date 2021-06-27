package dk.dren.hal.ctrl.events;

import lombok.Getter;

@Getter
public class WiegandEvent implements DeviceEvent {
    public static final int TYPE=0x01;
    private final int deviceId;
    private final int eventNumber;
    private final int bits;
    private final long data;

    public WiegandEvent(int deviceId, byte counter, byte[] data) {
        this.deviceId = deviceId;
        this.eventNumber = counter;
        this.bits = data[0];
        this.data = ((long)data[1]) & 0xff
                | ((((long)data[2]) & 0xff) << 8)
                | ((((long)data[3]) & 0xff) << (8*2))
                | ((((long)data[4]) & 0xff) << (8*3))
                | ((((long)data[5]) & 0xff) << (8*4))
                | ((((long)data[6]) & 0xff) << (8*5))
                | ((((long)data[7]) & 0xff) << (8*6));
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String toData() {
        StringBuilder sb = new StringBuilder();
        sb.append(deviceId).append("\t").append(TYPE).append("\t").append(eventNumber).append("\t");
        sb.append(String.format("Wiegand %d bits: 0x%x", bits, data));
        return sb.toString();
    }
}
