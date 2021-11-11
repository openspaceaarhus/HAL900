package dk.dren.hal.ctrl.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dk.dren.hal.ctrl.DoorMinderConfig;
import dk.dren.hal.ctrl.comms.BusDevice;
import dk.dren.hal.ctrl.comms.DoorMinder;
import dk.dren.hal.ctrl.crypto.AES256Key;
import dk.dren.hal.ctrl.events.ControllerStartEvent;
import dk.dren.hal.ctrl.events.DeviceEvent;
import dk.dren.hal.ctrl.halclient.HAL;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Starts a thread that periodically synchronizes the state with the local file system and HAL.
 * <p>
 * Everything is written to local storage so disconnected operation is possible
 * but once connection is reestablished the state is reconciled with HAL
 */
@Log
public class StateManager implements DoorMinder {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("y d/M h:m:s");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    public static final long MINIMUM_HAL_SYNC_INTERVAL = TimeUnit.SECONDS.toMillis(30);
    private static final long MINIMUM_EVENT_PUSH_INTERVAL = TimeUnit.SECONDS.toMillis(5);
    private final DoorMinderConfig config;
    private State state;
    private final Semaphore dirtyState = new Semaphore(1);
    private final Thread syncThread;
    private HAL _hal;

    /**
     * These are the events that have not yet been written to the FS
     */
    private final Map<Long, String> events = new TreeMap<>();
    private long lastStateSyncWithHAL;
    private long lastEventPush;

    public StateManager(DoorMinderConfig config) {
        this.config = config;
        loadFromFilesystem();
        recordEvent(new ControllerStartEvent());
        syncThread = new Thread(this::syncLoop);
        syncThread.setName("Sync thread");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void storeToFileSystem() throws IOException {
        ByteArrayOutputStream yamlBytes = new ByteArrayOutputStream();
        synchronized (this) {
            YAML.writeValue(yamlBytes, state);
        }

        File stateFile = config.getStateFile();
        if (isContentsOfFileDifferent(stateFile, yamlBytes.toByteArray())) {
            final File stateDir = stateFile.getParentFile();
            if (!stateDir.isDirectory()) {
                if (!stateDir.mkdirs()) {
                    log.warning("Unable to create missing directory: "+stateDir);
                }
            }
            File tmp = File.createTempFile("state-", ".yaml", stateDir);
            log.fine(() -> "Storing new " + stateFile);
            FileUtils.writeByteArrayToFile(tmp, yamlBytes.toByteArray());

            if (!tmp.renameTo(stateFile)) {
                throw new RuntimeException("Failed to rename " + tmp + " to " + stateFile);
            }
        }
    }

    private boolean isContentsOfFileDifferent(File file, byte[] content) throws IOException {
        if (!file.exists()) {
            return true;
        }
        final byte[] fileContent = FileUtils.readFileToByteArray(file);
        return !Arrays.equals(fileContent, content);
    }

    private void loadFromFilesystem() {
        File stateFile = config.getStateFile();
        if (stateFile.exists()) {
            try {
                synchronized (this) {
                    state = YAML.readValue(stateFile, State.class);
                }
                return;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed while loading " + stateFile, e);
            }
        }

        state = new State();
    }

    /**
     * Adds a newly enrolled bus device to the global state
     *
     * @param busDevice newly enrolled bus device
     */
    private void addDevice(BusDevice busDevice) {
        DeviceState ds = new DeviceState(busDevice.getId(),
                "New @" + millisToString(busDevice.getCreated()),
                AES256Key.store(busDevice.getSecretKey()));

        synchronized (this) {
            state.getDevices().put(ds.getId(), ds);
        }
        dirtyState.release(); // Signal to the sync thread that it's time to go to work.
    }

    public synchronized void addDevices(Map<Integer, BusDevice> deviceById) {
        for (Map.Entry<Integer, DeviceState> idAndState : state.getDevices().entrySet()) {
            if (!deviceById.containsKey(idAndState.getKey())) {
                deviceById.put(idAndState.getKey(), createBusDevice(idAndState.getValue()));
            }
        }
    }

    private BusDevice createBusDevice(DeviceState s) {
        return new BusDevice(s.getId(), AES256Key.load(s.getAesKey()), this);
    }

    private String millisToString(long millis) {
        synchronized (SIMPLE_DATE_FORMAT) {
            return SIMPLE_DATE_FORMAT.format(new Date(millis));
        }
    }

    @SneakyThrows
    private void syncLoop() {
        while (true) {
            final int dirt = dirtyState.drainPermits();
            log.fine(() -> "Syncing " + dirt + " changes");
            try {
                sync();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Got exception while syncing", e);
            }
            dirtyState.tryAcquire(MINIMUM_HAL_SYNC_INTERVAL, TimeUnit.MILLISECONDS); // Sleep until time has passed or there's work to be done.
        }
    }

    private void sync() throws IOException {
        final long now = System.currentTimeMillis();
        flushEventsToFileSystem();

        // Store state on file system
        storeToFileSystem();

        if (now-lastEventPush > MINIMUM_EVENT_PUSH_INTERVAL) {
            try {
                syncEventsToHal();
            } catch (Exception e) {
                _hal = null;
                log.log(Level.WARNING, "Failed to push events to HAL", e);
            }
            lastEventPush = now;
        }

        // Consolidate with HAL
        try {
            if (now - lastStateSyncWithHAL >= MINIMUM_HAL_SYNC_INTERVAL) {
                syncUsersFromHal();
                lastStateSyncWithHAL = now; // Yes, we want to keep the interval, even if the sync failed.

                // Store state on file system if HAL had changes
                storeToFileSystem();
            }
        } catch (Exception e) {
            _hal = null;
            log.log(Level.WARNING, "HAL sync failed, resetting hal connection", e);
            lastStateSyncWithHAL = now; // Yes, we want to keep the interval, even if the sync failed.
        }
    }

    private void flushEventsToFileSystem() {
        synchronized (events) {
            if (events.isEmpty()) {
                return;
            }
            final File eventsDir = config.getEventsFile().getParentFile();
            if (!eventsDir.isDirectory()) {
                if (!eventsDir.mkdirs()) {
                    log.warning("Unable to create missing directory: "+eventsDir);
                }
            }
            try (final FileWriter appender = new FileWriter(config.getEventsFile(), true)) {
                for (Map.Entry<Long, String> timestampAndEvent : events.entrySet()) {
                    appender.append(timestampAndEvent.getKey().toString())
                            .append("\t")
                            .append(timestampAndEvent.getValue())
                            .append("\n");
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed while appending "+events.size()+" events to " + config.getEventsFile(), e);
                return;
            }
            events.clear();
        }
    }

    private void syncEventsToHal() throws IOException {
        final File eventsFile = config.getEventsFile();
        File transmitting = new File(eventsFile.getParentFile(), eventsFile.getName() + ".transmitting");
        if (!transmitting.exists()) {
            synchronized (events) {
                if (eventsFile.exists() && eventsFile.length() > 0) {
                    if (!eventsFile.renameTo(transmitting)) {
                        throw new IOException("Failed to rename " + eventsFile + " to " + transmitting);
                    }
                } else {
                    return; // Nothing to do.
                }
            }
        }

        final byte[] contentToSend = FileUtils.readFileToByteArray(transmitting);
        if (getHAL().sendEvents(contentToSend)) {
            transmitting.delete();
        }
    }

    private void syncUsersFromHal() throws IOException {
        final HAL hal = getHAL();
        final State stateFromHal = hal.state();

        List<DeviceState> newDevices = new ArrayList<>();
        synchronized (this) {
            Set<Long> seen = new TreeSet<>();
            final Map<Long, String> rfidToPin = this.state.getRfidToPin();

            for (Map.Entry<Long, String> rfidAndPin : stateFromHal.getRfidToPin().entrySet()) {
                seen.add(rfidAndPin.getKey());
                final String oldPin = rfidToPin.get(rfidAndPin.getKey());
                if (oldPin == null) {
                    log.info(String.format("New rfid: %x", rfidAndPin.getKey()));
                    rfidToPin.put(rfidAndPin.getKey(), rfidAndPin.getValue());
                } else if (!oldPin.equals(rfidAndPin.getValue())) {
                    log.info(String.format("Changed pin for rfid: %x", rfidAndPin.getKey()));
                    rfidToPin.put(rfidAndPin.getKey(), rfidAndPin.getValue());
                }
            }

            final Iterator<Map.Entry<Long, String>> nuker = rfidToPin.entrySet().iterator();
            while (nuker.hasNext()) {
                final Map.Entry<Long, String> goner = nuker.next();
                if (!seen.contains(goner.getKey())) {
                    log.info(String.format("Removed rfid: %x", goner.getKey()));
                    nuker.remove();
                }
            }

            // Sync name of devices to the devices we know.
            for (DeviceState halDevice : stateFromHal.getDevices().values()) {
                if (halDevice.getId() == 0) {
                    continue;
                }
                final DeviceState myDevice = state.getDevices().get(halDevice.getId());
                if (myDevice == null) {
                    if (halDevice.getAesKey().isEmpty()) {
                        // Keeping my device.
                    } else {
                        state.getDevices().put(halDevice.getId(), halDevice);
                    }
                } else {
                    if (!myDevice.getName().equals(halDevice.getName())) {
                        log.info("New name for "+myDevice.getId()+" "+ myDevice.getName() + " -> "+halDevice.getName());
                        myDevice.setName(halDevice.getName());
                    }
                    if (halDevice.getAesKey().isEmpty()) {
                        newDevices.add(myDevice); // Update the aes key
                    }
                }
            }

            for (DeviceState myDevice : state.getDevices().values()) {
                if (!stateFromHal.getDevices().containsKey(myDevice.getId())) {
                    newDevices.add(myDevice);
                }
            }
        }

        if (!newDevices.isEmpty()) {
            getHAL().createDevices(newDevices);
        }
    }

    private HAL getHAL() throws IOException {
        if (_hal == null) {
            _hal = new HAL(config.getHalUri(), config.getHalUser(), config.getHalPassword());
            _hal.login();
        }
        return _hal;
    }

    @Override
    public BusDevice createBusDevice(int firstFreeNodeId, SecretKey secretKey) {
        final BusDevice busDevice = new BusDevice(firstFreeNodeId, secretKey, this);
        addDevice(busDevice);
        return busDevice;
    }

    @Override
    public void recordEvent(DeviceEvent event) {
        synchronized (events) {
            long timestamp = System.currentTimeMillis();
            while (events.containsKey(timestamp)) {
                timestamp++;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(event.getDeviceId()).append("\t")
                    .append(event.getType()).append("\t")
                    .append(event.getEventNumber()).append("\t");
            if (event.getData() != null) {
                sb.append(event.getData()).append("\t");
            }
            sb.append(event.getText());
            String eventAsString = sb.toString();
            log.info("New event: " + eventAsString);

            if (event.isLoggedRemotely()) {
                events.put(timestamp, eventAsString);
                dirtyState.release(); // Signal to the sync thread that it's time to go to work.
            }
        }
    }

    @Override
    public boolean validateCredentials(int deviceId, long rfid, String pin) {
        String okPin;
        synchronized (this) {
            okPin = state.getRfidToPin().get(rfid);
            if (okPin == null) {
                long strippedRfid = stripWg34(rfid);
                okPin = state.getRfidToPin().get(strippedRfid);
                if (okPin != null) {
                    log.info(() -> String.format("Did not find the raw rfd %x but did find the stripped version %x", rfid, strippedRfid));
                }
            }
        }

        if (okPin == null) {
            log.info(String.format("Unknown rfid: %d", rfid));
            return false; // Unknown rfid
        }

        // TODO: Check if this user has access to the door or not, at the moment only rfids with door access are exported.

        return pin.equals(okPin);
    }

    public static long stripWg34(long rfid) {
        return (rfid >> 1) & 0xffffffffL;
    }

}
