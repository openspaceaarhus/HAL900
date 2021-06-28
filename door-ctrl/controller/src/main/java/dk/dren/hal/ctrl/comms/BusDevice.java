package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.PollFrame;
import dk.dren.hal.ctrl.comms.frames.PollResponse;
import dk.dren.hal.ctrl.events.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

import javax.crypto.SecretKey;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Log
@RequiredArgsConstructor
public class BusDevice {
    public static final long PIN_ENTRY_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    @Getter
    private final int id;

    @Getter
    private final SecretKey secretKey;

    @Getter
    final private DoorMinder doorMinder;

    @Getter
    private int lastEventSeen;

    @Getter
    private long lastPollResponseSeen;

    @Getter
    private long created = System.currentTimeMillis();

    private long rfid = 0;
    private String pin = "";
    private long lastWiegandActivity = 0;

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
            if (rfid !=0 && lastPollResponseSeen-lastWiegandActivity > PIN_ENTRY_TIMEOUT) {
                sendEvent(new UserTimeout(id));
                resetInputState();
            }

            for (DeviceEvent event : pr.getEvents()) {
                lastEventSeen = event.getEventNumber();
                sendEvent(event);
                handleEvent(event);
            }
        }
    }

    private void sendEvent(DeviceEvent event) {
        doorMinder.recordEvent(event);
    }

    private void handleEvent(DeviceEvent event) {
        if (event instanceof PowerUpEvent) {
            resetInputState();
        } else if (event instanceof WiegandEvent) {
            final WiegandEvent we = (WiegandEvent) event;

            if (we.isRFID()) {
                resetInputState();
                rfid = we.getData();
                lastWiegandActivity = System.currentTimeMillis();
            } else if (rfid != 0) {
                if (we.isKeyPress()) {
                    if (we.getData() < 0 || we.getData() > 9) {
                        resetInputState();
                    } else {
                        lastWiegandActivity = System.currentTimeMillis();
                        pin += String.format("%d", we.getData());
                        if (pin.length() >= 5) {
                            tryPin();
                        }
                    }
                }
            }
        }
    }

    private void tryPin() {
        log.info(String.format("Trying %x@%s", rfid, pin));
        if (doorMinder.validateCredentials(id, rfid, pin)) {
            sendEvent(new Unlocked(id, rfid));



        }
    }

    private void resetInputState() {
        rfid = 0;
        pin = "";
    }
}
