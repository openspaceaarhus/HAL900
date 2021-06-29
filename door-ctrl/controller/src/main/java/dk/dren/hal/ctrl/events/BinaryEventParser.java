package dk.dren.hal.ctrl.events;

public class BinaryEventParser {

    public static DeviceEvent parse(int deviceId, int type, int counter, byte[] data) {
        if (type == PowerUpEvent.TYPE) {
            return new PowerUpEvent(deviceId, counter);
        } else if (type == WiegandEvent.TYPE) {
            return new WiegandEvent(deviceId, counter, data);
        } else if (type == ControlTokenEvent.TYPE) {
            return new ControlTokenEvent(deviceId, counter, data);
        } else if (type == ControlStateEvent.TYPE) {
            return new ControlStateEvent(deviceId, counter, data);
        } else {
            return new RawEvent(deviceId, type, counter, data);
        }
    }
}
