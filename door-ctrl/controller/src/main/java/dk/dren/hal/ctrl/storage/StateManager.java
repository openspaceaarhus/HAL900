package dk.dren.hal.ctrl.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dk.dren.hal.ctrl.comms.BusDevice;
import dk.dren.hal.ctrl.crypto.AES256Key;
import dk.dren.hal.ctrl.events.DeviceEvent;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Starts a thread that periodically synchronizes the state with the local file system and HAL.
 *
 * Everything is written to local storage so disconnected operation is possible
 * but once connection is reestablished the state is reconciled with HAL
 */
@Log
public class StateManager implements Consumer<DeviceEvent> {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("y d/m H:M:S");
    public static final ObjectMapper OM = new ObjectMapper(new YAMLFactory());
    private State state;
    private final Semaphore dirtyState = new Semaphore(1);
    private final File stateFile;
    private final Thread syncThread;

    public StateManager(File stateFile) {
        this.stateFile = stateFile;
        loadFromFilesystem();
        syncThread = new Thread(this::syncLoop);
        syncThread.setName("Sync thread");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void storeToFileSystem() throws IOException {
        File tmp = File.createTempFile("state-",".yaml", stateFile.getParentFile());
        log.fine(()->"Storing new "+stateFile);
        synchronized (OM) {
            OM.writeValue(tmp, state);
        }
        if (!tmp.renameTo(stateFile)) {
            throw new RuntimeException("Failed to rename "+tmp+" to "+stateFile);
        }
    }

    private void loadFromFilesystem() {
        if (stateFile.exists()) {
            try {
                synchronized (this) {
                    state = OM.readValue(stateFile, State.class);
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
    public void addDevice(BusDevice busDevice) {
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

    @Override
    public synchronized void accept(DeviceEvent deviceEvent) {
        long timestamp = System.currentTimeMillis();
        while (state.getEvents().containsKey(timestamp)) {
            timestamp++;
        }
        state.getEvents().put(timestamp, deviceEvent.toString());
        log.info("New event: "+deviceEvent.toData());
    }

    private BusDevice createBusDevice(DeviceState s) {
        final BusDevice busDevice = new BusDevice(s.getId(), AES256Key.load(s.getAesKey()));
        busDevice.setEventConsumer(this);
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
        // Store state on file system
        storeToFileSystem();

        // Consolidate with HAL

        // Store state on file system if HAL had changes
        storeToFileSystem();
    }
}
