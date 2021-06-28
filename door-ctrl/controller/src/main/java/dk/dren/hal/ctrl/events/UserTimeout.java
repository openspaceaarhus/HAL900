package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public String toData() {
        return deviceId + "\t" + getType() + "\t" + getEventNumber() + "\t" + "User timeout";
    }

}
