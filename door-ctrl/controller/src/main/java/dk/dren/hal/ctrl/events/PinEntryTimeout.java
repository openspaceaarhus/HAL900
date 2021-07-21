package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@RequiredArgsConstructor
public class PinEntryTimeout implements DeviceEvent {
    private final int deviceId;
    private final long rfid;

    @Override
    public int getType() {
        return 0xff;
    }

    @Override
    public int getEventNumber() {
        return 0xff;
    }

    @Override
    public Long getData() {
        return rfid;
    }

    @Override
    public String getText() {
        return "Pin Entry timeout";
    }

    @Override
    public boolean isLoggedRemotely() {
        return true;
    }
}
