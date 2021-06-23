package dk.dren.hal.ctrl.comms.frames;

import dk.dren.hal.ctrl.comms.ByteBuffer;
import dk.dren.hal.ctrl.comms.Frame;

public class PollFrame {
    public static final int TYPE = 0x00;

    public static Frame create(int targetId, int lastEventSeen) {

        final ByteBuffer payload = new ByteBuffer(1);
        payload.add((byte)lastEventSeen);

        return new Frame((byte)0x00, (byte)targetId, (byte)TYPE, payload);
    }
}
