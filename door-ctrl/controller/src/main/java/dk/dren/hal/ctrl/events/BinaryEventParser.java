package dk.dren.hal.ctrl.events;

public class BinaryEventParser {

    public static DeviceEvent parse(int deviceId, byte type, byte counter, byte[] data) {
        if (type == PowerUpEvent.TYPE) {
            return new PowerUpEvent(deviceId, counter);
        } else {
            return new RawEvent(deviceId, type, counter, data);
        }
    }
}
