package dk.dren.hal.ctrl.events;

public interface DeviceEvent {
    int getDeviceId();
    int getType();
    int getEventNumber();
    Long getData();
    String getText();
    boolean isLoggedRemotely();
}
