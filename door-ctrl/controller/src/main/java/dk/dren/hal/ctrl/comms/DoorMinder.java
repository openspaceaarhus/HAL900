package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.events.DeviceEvent;

import javax.crypto.SecretKey;

public interface DoorMinder {
    BusDevice createBusDevice(int firstFreeNodeId, SecretKey secretKey);
    void recordEvent(DeviceEvent event);
    boolean validateCredentials(int deviceId, long rfid, String pin);
}
