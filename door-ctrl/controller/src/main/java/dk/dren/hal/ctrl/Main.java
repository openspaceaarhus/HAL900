package dk.dren.hal.ctrl;

import dk.dren.hal.ctrl.comms.Poller;
import dk.dren.hal.ctrl.storage.StateManager;
import lombok.extern.java.Log;

import java.io.File;
import java.util.logging.Level;

@Log
public class Main {
    public static void main(String[] args) {
        log.info("Going to poll...");
        try {
            final File stateFile = new File("/tmp/state.yaml");
            final File serialDevice = new File("/dev/ttyUSB1");


            final StateManager stateManager = new StateManager(stateFile);

            Poller poller = new Poller(serialDevice, stateManager);

            poller.start();
            poller.join();

        } catch (Throwable e) {
            log.log(Level.SEVERE, "Fail!", e);
            System.exit(1);
        }
    }
}
