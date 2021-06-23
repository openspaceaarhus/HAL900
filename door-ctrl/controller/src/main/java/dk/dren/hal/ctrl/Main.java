package dk.dren.hal.ctrl;

import dk.dren.hal.ctrl.comms.Poller;
import lombok.extern.java.Log;

import java.io.File;
import java.util.logging.Level;

@Log
public class Main {
    public static void main(String[] args) {
        log.info("Going to poll...");
        try {
            Poller poller = new Poller(new File("/dev/ttyUSB1"));

            poller.start();

        } catch (Throwable e) {
            log.log(Level.SEVERE, "Fail!", e);
            System.exit(1);
        }
    }
}
