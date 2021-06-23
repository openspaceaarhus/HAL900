package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Frame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class EnrollRequest {
    public static final int TYPE = 0x01;
    public static final int TEMP_ID_SIZE = 4;

    private final Frame frame;

    public static EnrollRequest from(Frame frame) {
        if (frame.getPayload().size() != TEMP_ID_SIZE) {
            throw new IllegalArgumentException("Bad payload size "+frame.getPayload().size()+" should be "+TEMP_ID_SIZE);
        }
        return new EnrollRequest(frame);
    }

    public ByteBuffer getTemporaryId() {
        return frame.getPayload();
    }

    public String toString() {
        return "EnrollRequest-"+frame.toString();
    }
}
