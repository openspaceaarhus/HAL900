package dk.dren.hal.ctrl.comms;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import dk.dren.hal.ctrl.comms.frames.PollFrame;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
public class RS485 {
    public static final int BAUD = 19200;
    private final SerialPort commPort;
    private final Deframer deframer;
    private final byte[] transmitBuffer = new byte[512];
    private final Consumer<Frame> frameConsumer;
    private final Semaphore responseSemaphore = new Semaphore(1);
    private final int answerTimeoutMs;

    public RS485(File serialPort, Consumer<Frame> frameConsumer, int answerTimeoutMs) {
        this.frameConsumer = frameConsumer;
        commPort = SerialPort.getCommPort(serialPort.getAbsolutePath());
        this.answerTimeoutMs = answerTimeoutMs;

        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        commPort.setBaudRate(BAUD);
        commPort.setNumDataBits(8);
        commPort.setParity(SerialPort.NO_PARITY);
        commPort.setNumStopBits(1);
        commPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        //commPort.setRs485ModeParameters(true, true, 100, 100);
        if (!commPort.openPort()) {
            throw new RuntimeException("Could not open serial port: " + serialPort);
        }

        deframer = new Deframer(frame->{
            responseSemaphore.release();
            frameConsumer.accept(frame);
        });

        commPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                final byte[] receivedData = event.getReceivedData();
                if (receivedData.length != 0) {
                    for (byte data : receivedData) {
                        deframer.addByte(data);
                    }
                }
            }
        });
    }

    private int timeToTransmit(int bytes) {
        return (bytes*10*1000)/BAUD;
    }

    @SneakyThrows
    private boolean send(Frame frame, boolean waitForReply) {
        responseSemaphore.tryAcquire();
        commPort.clearRTS(); // Start driving the bus
        sleep(1);
        int bytes = frame.toBytes(transmitBuffer);
        final int written = commPort.writeBytes(transmitBuffer, bytes);
        if (written != bytes) {
            log.severe("Tried to write "+bytes+" but wrote "+written);
        }
        sleep(1+timeToTransmit(bytes)); // Approximate time it will take to send the data we just wrote.
        commPort.setRTS(); // Stop driving the bus

        logBytes(frame.getType() != PollFrame.TYPE? Level.FINE : Level.FINEST, transmitBuffer, bytes);

        if (waitForReply) {
            final long t0 = System.currentTimeMillis();
            if (responseSemaphore.tryAcquire(answerTimeoutMs, TimeUnit.MILLISECONDS)) {
                final long ms = System.currentTimeMillis() - t0;
                log.fine(()->"Got answer in " + ms + " ms");
                return true;
            } else {
                final long ms = System.currentTimeMillis() - t0;
                log.fine(()->"Got no answer, waited " + ms + " ms");
                return false;
            }
        } else {
            return true;
        }
    }

    private void logBytes(Level level, byte[] bytes, int count) {
        log.log(level, ()->{
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%d bytes:", count));
            for (int i = 0; i < count; i++) {
                sb.append(String.format(" %02x", bytes[i]));
            }
            return sb.toString();
        });
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendWithoutWait(Frame frame) {
        send(frame, false);
    }

    public boolean sendAndWaitForReply(Frame frame) {
        return send(frame, true);
    }

    public void close() {
        commPort.closePort();
    }
}
