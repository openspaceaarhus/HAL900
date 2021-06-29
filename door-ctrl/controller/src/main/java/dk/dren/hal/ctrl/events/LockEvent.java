package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LockEvent implements DeviceEvent {
    private final int deviceId;

    @Override
    public int getType() {
        return 0xff-3;
    }

    @Override
    public int getEventNumber() {
        return 0xff;
    }

    @Override
    public Long getData() {
        return null;
    }

    @Override
    public String getText() {
        return String.format("Locked");
    }

    @Override
    public boolean isLoggedRemotely() {
        return true;
    }
}
