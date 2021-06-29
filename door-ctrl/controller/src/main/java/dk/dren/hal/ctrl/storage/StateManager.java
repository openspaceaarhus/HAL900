package dk.dren.hal.ctrl.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dk.dren.hal.ctrl.DoorMinderConfig;
import dk.dren.hal.ctrl.comms.BusDevice;
import dk.dren.hal.ctrl.comms.DoorMinder;
import dk.dren.hal.ctrl.crypto.AES256Key;
import dk.dren.hal.ctrl.events.DeviceEvent;
import dk.dren.hal.ctrl.halclient.HAL;
import dk.dren.hal.ctrl.halclient.HalUser;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Starts a thread that periodically synchronizes the state with the local file system and HAL.
 *
 * Everything is written to local storage so disconnected operation is possible
 * but once connection is reestablished the state is reconciled with HAL
 */
@Log
public class StateManager implements DoorMinder {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("y d/m H:M:S");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private final DoorMinderConfig config;
    private State state;
    private final Semaphore dirtyState = new Semaphore(1);
    private final Thread syncThread;
    private HAL hal;

    /**
     * These are the events that have not yet been written to the FS
     */
    private final Map<Long, String> events = new TreeMap<>();

    public StateManager(DoorMinderConfig config) {
        this.config = config;
        loadFromFilesystem();
        syncThread = new Thread(this::syncLoop);
        syncThread.setName("Sync thread");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void storeToFileSystem() throws IOException {
        File stateFile = config.getStateFile();
        File tmp = File.createTempFile("state-",".yaml", stateFile.getParentFile());
        log.fine(()->"Storing new "+stateFile);
        synchronized (this) {
            YAML.writeValue(tmp, state);
        }
        if (!tmp.renameTo(stateFile)) {
            throw new RuntimeException("Failed to rename "+tmp+" to "+stateFile);
        }
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
                log.log(Level.SEVERE, "Failed while loading "+stateFile, e);
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
                "New @"+millisToString(busDevice.getCreated()),
                AES256Key.store(busDevice.getSecretKey()));

        synchronized (this) {
            state.getDevices().put(ds.getId(),ds);
        }
        dirtyState.release(); // Signal to the sync thread that it's time to go to work.
    }

    public synchronized List<BusDevice> getKnownBusDevices() {
        return state.getDevices().values().stream()
                .map(this::createBusDevice)
                .collect(Collectors.toList());
    }

    private BusDevice createBusDevice(DeviceState s) {
        final BusDevice busDevice = new BusDevice(s.getId(), AES256Key.load(s.getAesKey()), this);
        return busDevice;
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
            log.fine(()->"Syncing "+dirt+" changes");
            try {
                sync();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Got exception while syncing",e);
            }
            dirtyState.tryAcquire(30, TimeUnit.SECONDS); // Sleep until time has passed or there's work to be done.
        }
    }

    private void sync() throws IOException {
        flushEventsToFileSystem();
        // Store state on file system
        storeToFileSystem();

        // Consolidate with HAL
        HAL hal = connectToHAL();
        try {
            syncUsersFromHal();
        } catch (Exception e) {
            hal = null;
            log.log(Level.WARNING, "HAL sync failed, resetting hal connection", e);
        }

        // Store state on file system if HAL had changes
        storeToFileSystem();
    }

    private void flushEventsToFileSystem() {
        synchronized (events) {
            try (final FileWriter appender = new FileWriter(config.getEventsFile(), true)) {
                for (Map.Entry<Long, String> timestampAndEvent : events.entrySet()) {
                    appender.append(timestampAndEvent.getKey().toString())
                            .append("\t")
                            .append(timestampAndEvent.getValue())
                            .append("\n");
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed while appending to "+config.getEventsFile(), e);
                return;
            }
            events.clear();
        }
    }

    private void syncUsersFromHal() throws IOException {
        final List<HalUser> users = hal.users();
        synchronized (this) {
            Set<Long> seen = new TreeSet<>();
            final Map<Long, String> rfidToPin = state.getRfidToPin();
            for (HalUser halUser : users) {
                seen.add(halUser.getRfid());
                final String oldPin = rfidToPin.get(halUser.getRfid());
                if (oldPin == null) {
                    log.info(String.format("New rfid: %x", halUser.getRfid()));
                    rfidToPin.put(halUser.getRfid(), halUser.getPin());
                } else if (!oldPin.equals(halUser.getPin())) {
                    log.info(String.format("Changed pin for rfid: %x", halUser.getRfid()));
                    rfidToPin.put(halUser.getRfid(), halUser.getPin());
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
        }
    }

    private HAL connectToHAL() throws IOException {
        if (hal == null) {
            hal = new HAL(config.getHalUri(), config.getHalUser(), config.getHalPassword());
            hal.login();
        }
        return hal;
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
            final String eventAsString = event.toData();
            events.put(timestamp, eventAsString);
            log.info("New event: " + eventAsString);
            dirtyState.release(); // Signal to the sync thread that it's time to go to work.
        }
    }

    @Override
    public boolean validateCredentials(int deviceId, long rfid, String pin) {
        final String okPin;
        synchronized (this) {
            okPin = state.getRfidToPin().get(rfid);
        }

        if (okPin == null) {
            log.info(String.format("Unknown rfid: %d", rfid));
            return false; // Unknown rfid
        }

        // TODO: Check if this user has access to the door or not.

        return pin.equals(okPin);
    }
}
