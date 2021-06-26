package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.PollFrame;
import dk.dren.hal.ctrl.comms.frames.PollResponse;
import dk.dren.hal.ctrl.events.DeviceEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.crypto.SecretKey;
import java.util.function.Consumer;

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

    @Getter
    @Setter
    private Consumer<DeviceEvent> eventConsumer;

    /**
     * Produce the next output frame for this device
     *
     * @return
     */
    public Frame getQueryFrame() {
        return PollFrame.create(getId(), getLastEventSeen());
    }

    /**
     * Called whenever a frame is received from this device.
     *
     * @param frame The new frame
     */
    public void handleAnswerFrame(Frame frame) {
        if (frame.getType() == PollResponse.TYPE) {
            PollResponse pr = PollResponse.from(frame, id, secretKey);
            lastPollResponseSeen = System.currentTimeMillis();
            for (DeviceEvent event : pr.getEvents()) {
                lastEventSeen = event.getEventNumber();
                if (eventConsumer != null) {
                    eventConsumer.accept(event);
                }
            }
        }
    }
}
