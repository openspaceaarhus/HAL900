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
    private final byte[] dataBytes;

    @Override
    public Long getData() {
        return null;
    }

    @Override
    public String getText() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Raw event with %d bytes:", dataBytes.length));
        for (byte dataByte : dataBytes) {
            sb.append(String.format(" %02x", dataByte));
        }
        return sb.toString();
    }

    @Override
    public boolean isLoggedRemotely() {
        return false;
    }
}
