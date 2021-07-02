package dk.dren.hal.ctrl.halclient;

import dk.dren.hal.ctrl.storage.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HALTest {

    @Test
    public void login() throws IOException {
        final String passwd = System.getProperty("hal.passwd");
        if (passwd == null) {
            return;
        }

        final HAL hal = new HAL(URI.create("http://localhost"), "doorminder", passwd);
        hal.login();
        State state = hal.state();

        Assertions.assertNotNull(state);


    }
}