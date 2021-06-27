package dk.dren.hal.ctrl.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class RawEvent implements DeviceEvent {
    private final int deviceId;
    private final int type;
    private final int eventNumber;
    private final byte[] data;
    public String toData() {

        StringBuilder sb = new StringBuilder();

        sb.append(deviceId).append("\t").append(type).append("\t").append(eventNumber).append("\t");

        sb.append(String.format("Raw event with %d bytes:", data.length));
        String sep = "";
        for (int i = 0; i < data.length; i++) {
            sb.append(sep).append(String.format(" %02x", data[i]));
        }

        return sb.toString();
    }
}
