package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
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
    public Long getData() {
        return null;
    }

    @Override
    public String getText() {
        return String.format("New token: %x %x %x %x", token[3], token[2], token[1], token[0]);
    }

    @Override
    public boolean isLoggedRemotely() {
        return false;
    }
}
