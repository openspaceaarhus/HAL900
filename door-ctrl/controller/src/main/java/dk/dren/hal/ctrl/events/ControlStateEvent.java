package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ControlStateEvent implements DeviceEvent {
    public static final byte TYPE = 0x03;
    private final int deviceId;
    private final int eventNumber;
    private final byte state;

    public ControlStateEvent(int deviceId, int eventNumber, byte[] data) {
        this.deviceId = deviceId;
        this.eventNumber = eventNumber;
        state = data[0];
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public Long getData() {
        return ((long)state)&0xff;
    }

    @Override
    public String getText() {
        return String.format("New GPIO state: %x", state);
    }

    @Override
    public boolean isLoggedRemotely() {
        return true;
    }
}
