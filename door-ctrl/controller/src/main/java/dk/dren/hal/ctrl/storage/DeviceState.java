package dk.dren.hal.ctrl.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DeviceState {
    private int id;
    private String name;
    private String aesKey;
    private int outputs;
}
