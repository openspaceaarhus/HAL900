package dk.dren.hal.ctrl;

import dk.dren.hal.ctrl.comms.Frame;
import dk.dren.hal.ctrl.comms.RS485;
import dk.dren.hal.ctrl.comms.frames.PollFrame;
import lombok.extern.java.Log;

import java.io.File;
import java.util.logging.Level;

@Log
public class Main {
    public static void main(String[] args) {
        log.info("Going to poll...");
        try {
            RS485 rs485 = new RS485(new File("/dev/ttyUSB1"), frame -> {
                log.info("Got frame: " + frame);
            });

            int loop=0;
            while (true) {
                final Frame frame = PollFrame.create(0xff, 0x00);
                rs485.send(frame);
                Thread.sleep(100);
                if (loop++ > 100) {
                    log.info("Polling...");
                    loop = 0;
                }
            }
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Fail!", e);
            System.exit(1);
        }
    }
}
