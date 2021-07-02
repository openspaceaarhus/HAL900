package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@RequiredArgsConstructor
public class ControllerStartEvent implements DeviceEvent {
    @Override
    public int getDeviceId() {
        return 0;
    }

    @Override
    public int getType() {
        return 0xff-2;
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
        return "Controller Start";
    }

    @Override
    public boolean isLoggedRemotely() {
        return true;
    }
}
