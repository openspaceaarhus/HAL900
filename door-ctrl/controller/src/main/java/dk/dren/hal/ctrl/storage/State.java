package dk.dren.hal.ctrl.storage;

import dk.dren.hal.ctrl.events.DeviceEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The complete state of the controller, this will be stored locally as a cache as well as
 * on HAL where it will end up being stored in postgresql.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class State {
    /**
     * These are the known devices, created during enrollment and backed up on HAL
     */
    private Map<Integer, DeviceState> devices = new TreeMap<>();

    /**
     * rfid ids to the pin, this information comes only from HAL
     */
    private Map<String, String> rfidToPin = new TreeMap<>();

    /**
     * These are the events that have not yet been sent to HAL
     */
    private Map<Long, String> events = new TreeMap<>();
}