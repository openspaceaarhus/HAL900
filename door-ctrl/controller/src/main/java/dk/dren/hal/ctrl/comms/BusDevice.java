package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.ControlFrame;
import dk.dren.hal.ctrl.comms.frames.PollFrame;
import dk.dren.hal.ctrl.comms.frames.PollResponse;
import dk.dren.hal.ctrl.events.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

import javax.crypto.SecretKey;
import java.util.concurrent.TimeUnit;

@Log
@RequiredArgsConstructor
public class BusDevice {
    public static final long PIN_ENTRY_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final int OS_UNLOCK = 1;
    private static final int OS_WIEGAND_OK = 4;
    private static final int OS_OUTPUT_MASK = 0x0f;
    public static final int TIME_TO_KEEP_UNLOCKED = 20;

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

    @Setter
    @Getter
    private int externalOutputs = 0;

    private long rfid = 0;
    private String pin = "";
    private long lastWiegandActivity = 0;
    private byte[] controlToken;
    private int desiredOutputState;
    private byte currentOutputState;
    private long lockTime;
    private boolean outputPhase;



    /**
     * Produce the next output frame for this device
     *
     * @return
     */
    public Frame getQueryFrame() {
        if (lockTime != 0 && System.currentTimeMillis()-lockTime > TimeUnit.SECONDS.toMillis(TIME_TO_KEEP_UNLOCKED)) {
            doorMinder.recordEvent(new LockEvent(id));
            desiredOutputState = 0;
            lockTime = 0;
        }

        if (outputPhase) {
            outputPhase = false;
            final int combinedOutputstate = externalOutputs | desiredOutputState;
            if (combinedOutputstate != (currentOutputState&OS_OUTPUT_MASK) || controlToken == null) {
                log.fine(()->String.format("%x -> %x", desiredOutputState, (combinedOutputstate&OS_OUTPUT_MASK)));
                return ControlFrame.create(getId(), getLastEventSeen(), secretKey, controlToken, combinedOutputstate, 30, 0);
            }
        }

        outputPhase = true;
        return PollFrame.create(getId(), getLastEventSeen());
    }

    /**
     * Called whenever a frame is received from this device.
     *
     * @param frame The new frame
     */
    public void handleAnswerFrame(Frame frame) {
        checkPinEntryTimeout();

        if (frame.getType() == PollResponse.TYPE) {
            PollResponse pr = PollResponse.from(frame, id, secretKey);

            for (DeviceEvent event : pr.getEvents()) {
                if (lastEventSeen != event.getEventNumber()) {
                    lastEventSeen = event.getEventNumber();
                    sendEvent(event);
                    handleEvent(event);
                } else {
                    log.fine(()->"Ignoring duplicate event: "+lastEventSeen);
                }
            }
        }
    }

    private void checkPinEntryTimeout() {
        lastPollResponseSeen = System.currentTimeMillis();
        if (rfid !=0 && lastPollResponseSeen-lastWiegandActivity > PIN_ENTRY_TIMEOUT) {
            sendEvent(new PinEntryTimeout(id, rfid));
            resetInputState();
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
                        desiredOutputState = 0;
                    } else {
                        lastWiegandActivity = System.currentTimeMillis();
                        pin += String.format("%d", we.getData());
                        if (pin.length() >= 5) {
                            tryPin();
                        }
                    }
                }
            } else if (we.isKeyPress() && we.getData() > 9) {
                desiredOutputState = 0;
            }
        } else if (event instanceof ControlTokenEvent) {
            final ControlTokenEvent ctrlToken = (ControlTokenEvent) event;
            controlToken = ctrlToken.getToken();

        } else if (event instanceof ControlStateEvent) {
            final ControlStateEvent ctrlState = (ControlStateEvent) event;
            currentOutputState = ctrlState.getState();
        }
    }

    private void tryPin() {
        log.fine(()->String.format("Trying %x@%s", rfid, pin));
        if (doorMinder.validateCredentials(id, rfid, pin)) {
            sendEvent(new Unlocked(id, rfid));
            desiredOutputState = OS_UNLOCK | OS_WIEGAND_OK;
            lockTime = System.currentTimeMillis()+ TIME_TO_KEEP_UNLOCKED;
            resetInputState();
        }
    }

    private void resetInputState() {
        rfid = 0;
        pin = "";
    }
}
