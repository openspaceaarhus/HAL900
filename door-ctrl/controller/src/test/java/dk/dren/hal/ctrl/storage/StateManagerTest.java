package dk.dren.hal.ctrl.storage;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateManagerTest {

    @Test
    void stripWg34() {
        Assert.assertEquals(2426005L,StateManager.stripWg34(8594786602L));
    }
}