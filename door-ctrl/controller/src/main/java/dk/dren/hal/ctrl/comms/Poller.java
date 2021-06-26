package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.EnrollRequest;
import dk.dren.hal.ctrl.comms.frames.EnrollResponse;
import dk.dren.hal.ctrl.comms.frames.PollFrame;
import dk.dren.hal.ctrl.storage.StateManager;
import lombok.extern.java.Log;

import java.io.File;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Polls all the known devices and enrolls new ones.
 */
@Log
public class Poller {

    public static final long ENROLLMENT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private final RS485 rs485;
    private final StateManager stateManager;
    private Thread pollThread;
    private Map<Integer, BusDevice> deviceById = new TreeMap<>();
    private EnrollRequest enrollmentRequest;
    private List<EnrollResponse> pendingEnrollments = new ArrayList<>();

    public Poller(File serialDevice, StateManager stateManager) {
        rs485 = new RS485(serialDevice, this::handleFrame);
        this.stateManager = stateManager;
        for (BusDevice knownBusDevice : stateManager.getKnownBusDevices()) {
            deviceById.put(knownBusDevice.getId(), knownBusDevice);
        };
    }

    private void handleFrame(Frame frame) {
        log.fine(()->"Got: "+frame.toString());
        if (frame.getType() == EnrollRequest.TYPE) {
            handleEnrollRequest(frame);
        } else if (frame.getTargetId() == 0x00){
            final BusDevice busDevice = deviceById.get((int)frame.getSourceId());
            if (busDevice != null) {
                busDevice.handleAnswerFrame(frame);
            } else {
                log.warning(String.format("Unknown source device: 0x%02x: %s", frame.getSourceId(), frame));
            }
        } else {
            log.warning(String.format("Unknown target device: 0x%02x: %s", frame.getTargetId(), frame));
        }
    }

    private void handleEnrollRequest(Frame frame) {
        enrollmentRequest = EnrollRequest.from(frame);
    }

    public void start() {
        pollThread = new Thread(this::poll);
        pollThread.setName("Poller");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void poll() {
        while (true) {
            for (BusDevice bd : deviceById.values()) {
                rs485.sendAndWaitForReply(bd.getQueryFrame());
            }

            final Iterator<EnrollResponse> pendingIterator = pendingEnrollments.iterator();
            while (pendingIterator.hasNext()) {
                EnrollResponse pe = pendingIterator.next();
                final BusDevice bd = pe.getBusDevice();
                if (bd.getCreated() < bd.getLastPollResponseSeen()) {
                    pendingIterator.remove();
                    stateManager.addDevice(bd);
                    log.info("Successful enrollment of device #"+bd.getId());
                } else if (System.currentTimeMillis()-bd.getCreated() > ENROLLMENT_TIMEOUT) {
                    deviceById.remove(bd.getId());
                    pendingIterator.remove();
                    log.info("Timed out enrollment of device #"+bd.getId());
                } else {
                    rs485.sendWithoutWait(pe.getFrame());
                }
            }

            // Special handling of enrollment
            enrollmentRequest = null;
            rs485.sendAndWaitForReply(PollFrame.create(0xff, 0));
            if (enrollmentRequest != null) {
                log.info("Got "+ enrollmentRequest);
                final EnrollResponse enrollResponse = EnrollResponse.create(enrollmentRequest, getFirstFreeNodeId());
                rs485.sendAndWaitForReply(enrollResponse.getFrame());
                pendingEnrollments.add(enrollResponse);
                deviceById.put(enrollResponse.getBusDevice().getId(), enrollResponse.getBusDevice());
            }
        }
    }

    private int getFirstFreeNodeId() {
        Set<Integer> used = new TreeSet<>();
        used.addAll(deviceById.keySet());
        for (EnrollResponse pendingEnrollment : pendingEnrollments) {
            used.add(pendingEnrollment.getBusDevice().getId());
        }

        for (int i=1;i<=250;i++) {
            if (!used.contains(i)) {
                return i;
            }
        }

        throw new IllegalStateException("Cannot find a free node id in the range 1..250");
    }

    public void join() throws InterruptedException {
        pollThread.join();
    }
}
