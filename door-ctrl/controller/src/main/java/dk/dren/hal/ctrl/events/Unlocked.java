package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
    public String toData() {
        return deviceId + "\t" + getType() + "\t" + getEventNumber() + "\t"+rfid+"\t" + String.format("Unlocked for rfid:%x", rfid);
    }
}
