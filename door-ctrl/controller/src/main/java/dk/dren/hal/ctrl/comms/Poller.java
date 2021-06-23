package dk.dren.hal.ctrl.comms;

import dk.dren.hal.ctrl.comms.frames.EnrollRequest;
import dk.dren.hal.ctrl.comms.frames.EnrollResponse;
import dk.dren.hal.ctrl.comms.frames.PollFrame;
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
    private Thread pollThread;
    private Map<Integer, BusDevice> deviceById = new TreeMap<>();
    private EnrollRequest enrollmentRequest;
    private Semaphore enrollRequestSemaphore = new Semaphore(1);
    private List<EnrollResponse> pendingEnrollments = new ArrayList<>();

    public Poller(File serialDevice) {
        rs485 = new RS485(serialDevice, this::handleFrame);
    }

    private void handleFrame(Frame frame) {
        if (frame.getType() == EnrollRequest.TYPE) {
            handleEnrollRequest(frame);
        } else {
            final BusDevice busDevice = deviceById.get((int)frame.getSourceId());
            if (busDevice != null) {
                busDevice.handleAnswerFrame(frame);
            } else {
                log.warning(String.format("Unknown source device: 0x%02x: %s", frame.getSourceId(), frame));
            }
        }
    }

    private void handleEnrollRequest(Frame frame) {
        enrollmentRequest = EnrollRequest.from(frame);
        enrollRequestSemaphore.release();
    }

    private void awaitEnrollAnswerFrame() throws InterruptedException {
        enrollRequestSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS);
    }

    public void start() {
        pollThread = new Thread(this::poll);
        pollThread.setName("Poller");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void poll() {
        while (true) {
            try {
                for (BusDevice bd : deviceById.values()) {
                    rs485.send(bd.getQueryFrame());
                    bd.awaitAnswerFrame(100);
                }

                final Iterator<EnrollResponse> pendingIterator = pendingEnrollments.iterator();
                while (pendingIterator.hasNext()) {
                    EnrollResponse pe = pendingIterator.next();
                    final BusDevice bd = pe.getBusDevice();
                    if (bd.getCreated() < bd.getLastPollResponseSeen()) {
                        pendingIterator.remove();
                        storeDevices();
                        log.info("Successful enrollment of device #"+bd.getId());
                    } else {
                        if (System.currentTimeMillis()-bd.getCreated() > ENROLLMENT_TIMEOUT) {
                            deviceById.remove(bd.getId());
                            pendingIterator.remove();
                            log.info("Timed out enrollment of device #"+bd.getId());
                        }
                    }
                }

                // Special handling of enrollment
                enrollmentRequest = null;
                enrollRequestSemaphore.tryAcquire(); // Either we get it or we don't doesn't matter.
                rs485.send(PollFrame.create(0xff, 0));
                awaitEnrollAnswerFrame();
                if (enrollmentRequest != null) {
                    log.info("Got "+ enrollmentRequest);
                    final EnrollResponse enrollResponse = EnrollResponse.create(enrollmentRequest, getFirstFreeNodeId());
                    rs485.send(enrollResponse.getFrame());
                    pendingEnrollments.add(enrollResponse);
                    deviceById.put(enrollResponse.getBusDevice().getId(), enrollResponse.getBusDevice());
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Got interrupted", e);
            }
        }
    }

    private void storeDevices() {
        // TODO
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
}
