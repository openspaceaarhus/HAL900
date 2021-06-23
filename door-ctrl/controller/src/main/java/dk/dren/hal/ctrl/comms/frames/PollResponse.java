package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.Frame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PollResponse {
    public static final int TYPE = 0x04;
    private final Frame frame;

    public static PollResponse from(Frame frame) {
        return new PollResponse(frame);
    }
}
