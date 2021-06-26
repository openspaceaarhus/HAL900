package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.PollFrame;
import dk.dren.hal.ctrl.comms.frames.PollResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.util.concurrent.Semaphore;

@RequiredArgsConstructor
public class BusDevice {
    @Getter
    private final int id;

    @Getter
    private final SecretKey secretKey;

    @Getter
    private int lastEventSeen;

    @Getter
    private long lastPollResponseSeen;

    @Getter
    private long created = System.currentTimeMillis();

    /**
     * Produce the next output frame for this device
     * @return
     */
    public Frame getQueryFrame() {
        // TODO: Figure how to poll the device.
        return PollFrame.create(getId(), getLastEventSeen());
    }

    /**
     * Called whenever a frame is received from this device.
     * @param frame The new frame
     */
    public void handleAnswerFrame(Frame frame) {
         if (frame.getType() == PollResponse.TYPE) {
            PollResponse pr = PollResponse.from(frame);
            lastPollResponseSeen = System.currentTimeMillis();

        }

        // TODO something with the frame...
    }
}
