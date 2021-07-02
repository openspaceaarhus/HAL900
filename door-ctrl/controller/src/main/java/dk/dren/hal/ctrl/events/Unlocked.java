package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
@Getter
public class Unlocked implements DeviceEvent {
    private final int deviceId;
    private final long rfid;

    @Override
    public int getType() {
        return 0xff-1;
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
        return String.format("Unlocked for rfid:%x", rfid);
    }

    @Override
    public boolean isLoggedRemotely() {
        return true;
    }
}
