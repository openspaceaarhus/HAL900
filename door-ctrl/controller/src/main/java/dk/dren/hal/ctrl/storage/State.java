package dk.dren.hal.ctrl.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Map<Integer, DeviceState> devices = new TreeMap<>();
    private Map<Long, LogEvent> events = new TreeMap<>();
    private Map<String,String> rfidToPin = new TreeMap<>();
}
