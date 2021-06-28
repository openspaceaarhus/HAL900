package dk.dren.hal.ctrl.halclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data

public class HalUser {
    @JsonProperty
    private Long rfid;
    @JsonProperty
    private String pin;
}
