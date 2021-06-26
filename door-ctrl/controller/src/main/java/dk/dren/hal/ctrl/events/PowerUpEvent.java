package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class PowerUpEvent implements DeviceEvent {
    public static final int TYPE=0x00;
    private final int deviceId;
    private final int eventNumber;

    @Override
    public int getType() {
        return TYPE;
    }

    public String toData() {
        return deviceId + "\t" + TYPE + "\t" + eventNumber + "\t" + "Power up";
    }
}
