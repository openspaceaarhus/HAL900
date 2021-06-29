package dk.dren.hal.ctrl.comms;

import lombok.extern.java.Log;

import javax.print.attribute.standard.MediaSize;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.zip.CRC32;

/**
 *
 * The purpose of this class is to pick valid frames out of the stream of bytes received from the rs485 bus, they
 * look like this:
 *
 * | Size   | Meaning |
 * | ------ |: ------:|
 * | 1      | Start of message, always 0xf0 |
 * | 1      | source id (0x00 is controller, 0xff is discovery) |
 * | 1      | target id (0x00 is controller, 0xff is discovery) |
 * | 1      | message type |
 * | 1      | psize=payload size |
 * | psize  | (encrypted) payload |
 * | 4      | CRC32 of all previous bytes, except for the start-of-message byte
 * | 1      | End of message, always 0xf1 |
 *
 */
@Log
public class Deframer {

    private final Consumer<Frame> frameConsumer;
    private final ByteBuffer buffer = new ByteBuffer(64*1024);

    /**
     * Creates a new deframer
     *
     * @param frameConsumer Consumer that's handed all the parsed frames
     */
    public Deframer(Consumer<Frame> frameConsumer) {
        this.frameConsumer = frameConsumer;
    }

    /**
     * Parses a single byte
     *
     * @param input The byte read from the spa controller
     */
    public void addByte(byte input) {
        buffer.add(input);
        if (input == Frame.END_SENTINEL || buffer.isFull()) {
            try {
                tryParse();
            } catch (Throwable e) {
                log.log(Level.SEVERE, "Ignoring exception while trying to parse buffer", e);
            }
        }
    }

    /**
     * Attempts to parse a frame, this consumes data from the buffer and calls the frame consumer with
     * any frames found.
     */
    private void tryParse() {
        // First eat bytes until a START_CENTINEL is found or we run out of input
        while (!buffer.isEmpty() && buffer.get(Frame.START_SENTINEL_INDEX) != Frame.START_SENTINEL) {
            buffer.remove();
        }

        if (buffer.isEmpty()) {
            log.fine(()->"Ran out of input before start sentinel");
            return; // Didn't find a start-of-message here.
        }

        assert(buffer.get(Frame.START_SENTINEL_INDEX) == Frame.START_SENTINEL);

        if (buffer.size() < Frame.MINIMUM_BYTES_IN_FRAME) {
            log.fine(()->"Not enough bytes yet "+buffer.size()+" < "+ Frame.MINIMUM_BYTES_IN_FRAME);
            return; // Not enough bytes to make up a full frame, but there was a start sentinel, so just punt
        }

        final int payloadSize = buffer.get(Frame.PAYLOAD_SIZE_INDEX) & 0xff;
        if (payloadSize < 0) {
            throw new IllegalArgumentException("Payload size should not be negative: "+payloadSize);
        }
        if (buffer.size() < Frame.MINIMUM_BYTES_IN_FRAME+payloadSize) {
            log.fine(()->"Not enough bytes yet "+buffer.size()+" < "+ Frame.MINIMUM_BYTES_IN_FRAME+payloadSize+" payload="+payloadSize);
            return; // Not enough bytes to make up a full frame including payload
        }

        final int endSentinelIndex = Frame.getEndSentinelIndex(payloadSize);
        if (endSentinelIndex < 0) {
            throw new IllegalArgumentException("End sentinel index should not be negative: "+endSentinelIndex);
        }
        final byte endSentinel = buffer.get(endSentinelIndex);
        if (endSentinel != Frame.END_SENTINEL) {
            log.fine(()->"Did not find an end sentinel at offset "+endSentinelIndex+": "+endSentinel+" != "+ Frame.END_SENTINEL);
            buffer.remove(); // Just skip the start sentinel, because there's no ending sentinel to match it.
            tryParse(); // Retry without the false start sentinel.
            return;
        }

        log.finer(()->logBytes(buffer, Frame.START_SENTINEL_INDEX, endSentinelIndex));

        final int crc32Index = Frame.getCrc32Index(payloadSize);
        final long crc32FromFrame = read32bitLittleEndian(buffer, crc32Index);
        CRC32 crc32 = new CRC32();
        for (int i = Frame.START_SENTINEL_INDEX+1; i < crc32Index; i++) {
            crc32.update(buffer.get(i));
        }
        if (crc32FromFrame != crc32.getValue()) {
            log.info(()->String.format("Bad crc: %08x vs %08x",
                    crc32FromFrame, crc32.getValue()));
            buffer.remove(); // Just skip the start sentinel, because the crc didn't match, so we might just be out of sync
            tryParse(); // Retry without the false start sentinel.
            return;
        }

        // At this point we know there's a valid frame sitting at the start of the buffer, so we can parse out the details
        // and pass them off to the consumer as a Frame
        final byte sourceId = buffer.get(Frame.SOURCE_ID_INDEX);
        final byte targetId = buffer.get(Frame.TARGET_ID_INDEX);
        final byte messageType = buffer.get(Frame.MESSAGE_TYPE_INDEX);
        final ByteBuffer payload = buffer.copy(Frame.PAYLOAD_INDEX, payloadSize);

        final Frame frame = new Frame(sourceId, targetId, messageType, payload);
        try {
            frameConsumer.accept(frame);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while handling frame "+frame+": "+e);
        }

        // Remove all the bytes we just parsed
        buffer.remove(endSentinelIndex+1);

        tryParse(); // Keep going, in case there are more frames buffered
    }

    private String logBytes(ByteBuffer buffer, int start, int end) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%d bytes:", end-start));
        String sep = "";
        for (int i = start; i <= end; i++) {
            sb.append(sep).append(String.format("%02x", buffer.get(i)));
            sep = ",";
        }

        return sb.toString();
    }

    public static long read32bitLittleEndian(ByteBuffer buffer, int index) {

        long result = ((long)buffer.get(index)) & 0xff;
        result |= ((long)buffer.get(index+1) & 0xff) << 8;
        result |= ((long)buffer.get(index+2) & 0xff) << 16;
        result |= ((long)buffer.get(index+3) & 0xff) << 24;

        return result;
    }
}
