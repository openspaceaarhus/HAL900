package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ControlTokenEvent implements DeviceEvent {
    public static final byte TYPE = 0x4;
    private final int deviceId;
    private final int eventNumber;
    private final byte[] token;

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String toData() {
        StringBuilder sb = new StringBuilder();
        sb.append(deviceId).append("\t").append(TYPE).append("\t").append(eventNumber).append("\t");
        sb.append(String.format("New token: %x %x %x %x", token[0], token[1], token[2], token[3]));
        return sb.toString();
    }
}
