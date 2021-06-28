package dk.dren.hal.ctrl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URI;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DoorMinderConfig {
    public static final ObjectMapper OM = new ObjectMapper(new YAMLFactory());

    private File stateFile;
    private File serialDevice;
    private URI halUri = URI.create("https://hal.osaa.dk");
    private String halUser = "doorminder";
    private String halPassword;

    public static DoorMinderConfig load(File configFile) throws IOException {
        return OM.readValue(configFile, DoorMinderConfig.class);
    }
}
