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

    /**
     * The file containing information about the access devices and the pin codes for rfids,
     * this is what's needed to be able to run in off-line mode, so it needs to live on flash
     * if working after a reboot in off-line mode is needed.
     *
     * It's only written when device or RFID data changes, so it should not eat the flash.
     */
    private File stateFile;

    /**
     * This is the temp file that events are appended to, before being sent to HAL, it needs to live in memory
     * to avoid eating the flash.
     *
     * With this on tmpfs it's possible to restart the controller without losing events, but not to reboot.
     */
    private File eventsFile;

    /**
     * The serial device that the RS485 adaptor lives on
     */
    private File serialDevice;

    /**
     * The HAL server to integrate with
     */
    private URI halUri = URI.create("https://hal.osaa.dk");

    /**
     * The user to use for making API calls to the HAL server
     */
    private String halUser = "doorminder";

    /**
     * The password used to authenticate the halUser
     */
    private String halPassword;

    /**
     * True if enrollment is enabled.
     *
     * Enrollment is insecure and it slows down the poll-loop, so it should not be enabled "in production".
     */
    private boolean enrollEnabled = false;

    /**
     * The maximum amount of time to wait for a device to answer a request, in milliseconds.
     */
    private int pollTimeout = 100;

    public static DoorMinderConfig load(File configFile) throws IOException {
        return OM.readValue(configFile, DoorMinderConfig.class);
    }
}
