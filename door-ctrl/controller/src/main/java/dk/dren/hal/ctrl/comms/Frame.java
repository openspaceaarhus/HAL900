package dk.dren.hal.ctrl.comms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.util.function.Supplier;
import java.util.zip.CRC32;

/**
 * This is the raw information known about a frame of data recovered from the bus
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
 */
@Log
@Getter
@RequiredArgsConstructor
public class Frame {
    public static final byte START_SENTINEL = (byte)0xf0;
    public static final byte END_SENTINEL = (byte)0xf1;
    public static final int MINIMUM_BYTES_IN_FRAME = 10;
    public static final int START_SENTINEL_INDEX = 0;
    public static final int SOURCE_ID_INDEX = 1;
    public static final int TARGET_ID_INDEX = 2;
    public static final int MESSAGE_TYPE_INDEX = 3;
    public static final int PAYLOAD_SIZE_INDEX = 4;
    public static final int PAYLOAD_INDEX = 5;
    public static final int CRC32_SIZE = 4;

    public static final int MT_ENROLL_RESPONSE = 0x02;
    public static final int MT_POLL_ACK = 0x03;
    public static final int MT_POLL_RESPONSE = 0x04;


    private final byte sourceId;
    private final byte targetId;
    private final byte type;
    private final ByteBuffer payload;

    public static int getCrc32Index(byte payloadSize) {
        return Frame.PAYLOAD_INDEX + payloadSize;
    }

    public static int getEndSentinelIndex(byte payloadSize) {
        return getCrc32Index(payloadSize) + CRC32_SIZE;
    }

    public int toBytes(byte[] transmitBuffer) {
        transmitBuffer[START_SENTINEL_INDEX] = START_SENTINEL;
        transmitBuffer[SOURCE_ID_INDEX] = getSourceId();
        transmitBuffer[TARGET_ID_INDEX] = getTargetId();
        transmitBuffer[MESSAGE_TYPE_INDEX] = getType();

        final ByteBuffer payload = getPayload();
        final int payloadSize = payload.size();
        transmitBuffer[PAYLOAD_SIZE_INDEX] = (byte) payloadSize;
        for (int i = 0; i< payloadSize; i++) {
            transmitBuffer[PAYLOAD_INDEX +i] = payload.get(i);
        }

        final int crc32Index = getCrc32Index((byte) payloadSize);

        CRC32 crc32 = new CRC32();
        for (int i = START_SENTINEL_INDEX +1; i < crc32Index; i++) {
            crc32.update(transmitBuffer[i]);
        }
        final long crc32value = crc32.getValue();

        //log.info(()->String.format("crc: %x", crc32value));
        transmitBuffer[crc32Index+0] = (byte)(crc32value & 0xff);
        transmitBuffer[crc32Index+1] = (byte)((crc32value>>8) & 0xff);
        transmitBuffer[crc32Index+2] = (byte)((crc32value>>16) & 0xff);
        transmitBuffer[crc32Index+3] = (byte)((crc32value>>24) & 0xff);

        transmitBuffer[getEndSentinelIndex((byte)payloadSize)] = END_SENTINEL;

        return MINIMUM_BYTES_IN_FRAME +payloadSize;
    }

    /*
      public Payload parsePayload() {
          final PayloadTypeDescription description = getDescription();
          if (description == null) {
              throw new IllegalArgumentException(String.format("Unknown message type: %02x", type));
          }
          if (description.getParser() == null) {
              throw new IllegalArgumentException("No parser for "+description);
          }
          return description.getParser().apply(this);
      }

      public <T extends Payload> T parsePayload(Class<T> theClass) {
          final Payload payload = parsePayload();
          if (theClass.isAssignableFrom(payload.getClass())) {
              return (T)payload;
          } else {
              throw new IllegalArgumentException("Wrong type "+payload.getClass()+" is not "+theClass);
          }
      }
  */
    @Override
    public String toString() {
        return String.format("Frame: src:%02x, tgt:%02x, type:%02x, payload:%s",
                sourceId, targetId, type, arrayToString());
    }

    private String arrayToString() {
        if (payload.isEmpty()) {
            return "Empty";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%d bytes:", payload.size()));
        for (int i = 0; i < payload.size(); i++) {
            sb.append(String.format(" %02x", payload.get(i)));
        }
        return sb.toString();
    }
/*
    public byte getPayload(int byteIndex) {
        return getPayload()[byteIndex];
    }

    public boolean bit(int byteIndex, int bit) {
        return (getPayload(byteIndex) & (1<<bit)) != 0;
    }

    public byte bits(int byteIndex, int bitIndex, int bitCount) {
        return (byte)((getPayload(byteIndex) >> bitIndex) & ((1<<bitCount) -1));
    }

     */
}
