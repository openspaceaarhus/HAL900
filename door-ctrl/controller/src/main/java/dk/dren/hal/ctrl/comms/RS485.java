package dk.dren.hal.ctrl.comms;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.extern.java.Log;

import java.io.File;
import java.util.function.Consumer;
@Log
public class RS485 {
    public static final int BAUD = 19200;
    private final SerialPort commPort;
    private final Deframer deframer;
    private final byte[] transmitBuffer = new byte[512];

    public RS485(File serialPort, Consumer<Frame> frameConsumer) {
        commPort = SerialPort.getCommPort(serialPort.getAbsolutePath());

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


        deframer = new Deframer(frameConsumer);

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

    public void send(Frame frame) {
        commPort.clearRTS();
        sleep(1);
        int bytes = frame.toBytes(transmitBuffer);
        final int written = commPort.writeBytes(transmitBuffer, bytes);
        if (written != bytes) {
            log.severe("Tried to write "+bytes+" but wrote "+written);
        }
        sleep(1+timeToTransmit(bytes));
        commPort.setRTS();

        logBytes(transmitBuffer, bytes);
    }

    private void logBytes(byte[] bytes, int count) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%d bytes:", count));
        for (int i = 0; i < count; i++) {
            sb.append(String.format(" %02x", bytes[i]));
        }
        log.info(sb.toString());
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
