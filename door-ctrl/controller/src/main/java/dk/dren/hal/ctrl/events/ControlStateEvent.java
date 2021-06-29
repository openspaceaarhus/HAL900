package dk.dren.hal.ctrl.events;

import lombok.Getter;

@Getter
public class ControlStateEvent implements DeviceEvent {
    public static final byte TYPE = 0x03;
    private final int deviceId;
    private final int eventNumber;
    private final byte state;

    public ControlStateEvent(int deviceId, byte eventNumber, byte[] data) {
        this.deviceId = deviceId;
        this.eventNumber = eventNumber;
        state = data[0];
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String toData() {
        StringBuilder sb = new StringBuilder();
        sb.append(deviceId).append("\t").append(TYPE).append("\t").append(eventNumber).append("\t");
        sb.append(String.format("New GPIO state: %x", state));
        return sb.toString();
    }
}
