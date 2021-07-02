package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@RequiredArgsConstructor
public class UserTimeout implements DeviceEvent {
    private final int deviceId;

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
        return null;
    }

    @Override
    public String getText() {
        return "User timeout";
    }

    @Override
    public boolean isLoggedRemotely() {
        return true;
    }
}
