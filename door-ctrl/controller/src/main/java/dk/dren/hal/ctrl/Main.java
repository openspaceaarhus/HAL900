package dk.dren.hal.ctrl;

import dk.dren.hal.ctrl.comms.Poller;
import dk.dren.hal.ctrl.storage.StateManager;
import lombok.extern.java.Log;

import java.io.File;
import java.util.logging.Level;

@Log
public class Main {
    public static void main(String[] args) {
        try {
            final File configFile = new File(System.getProperty("user.home")+"/doorminder.yaml");
            log.info("Loading config from "+configFile);
            DoorMinderConfig doorMinderConfig = DoorMinderConfig.load(configFile);
            final StateManager stateManager = new StateManager(doorMinderConfig);

            Poller poller = new Poller(doorMinderConfig.getSerialDevice(), stateManager, doorMinderConfig.getPollTimeout(), doorMinderConfig.isEnrollEnabled());

            poller.start();
            poller.join();
            log.warning("Poller exited");

        } catch (Throwable e) {
            log.log(Level.SEVERE, "Fail!", e);
            System.exit(1);
        }
    }
}
