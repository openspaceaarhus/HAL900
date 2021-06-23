#include "frame.h"
#include "aes256cbc.h"
#include "crc32.h"
#include "uart.h"
#include "board.h"

// Just a counter of received messages
uint16_t rxCount = 0;

uint8_t nodeId;
uint8_t nodeKey[AES_KEY_SIZE];

uint16_t frameRxCount(void) {
  return rxCount;
}

void frameInit(void) {
  nodeId = 0xff;
  // TODO: Load nodeId and nodeKey from EEPROM
  GPOUTPUT(RS485_LED);
}

uint8_t createPollAck(uint8_t *buffer, uint8_t targetId) {
  uint8_t payloadSize = 0;
  
  buffer[SOURCE_ID_INDEX] = nodeId;
  buffer[TARGET_ID_INDEX] = targetId;
  buffer[MESSAGE_TYPE_INDEX] = MT_POLL_ACK;
  buffer[PAYLOAD_SIZE_INDEX] = payloadSize;
  uint8_t crcIndex = PAYLOAD_INDEX+payloadSize;
  uint32_t actualCrc = crc32(buffer, crcIndex);
  *(uint32_t *)(buffer + crcIndex) = actualCrc;
  buffer[crcIndex+4] = END_SENTINEL;
  return crcIndex+5;
}


uint8_t handlePoll(uint8_t *buffer, uint8_t sourceId, uint8_t targetId, uint8_t lastEvent) {  
  return createPollAck(buffer, sourceId);   
}


/**
 * Returns the number of bytes placed in the buffer to transmit. 
 **/
uint8_t handleFrame(uint8_t* buffer, uint8_t bufferInUse) {
  uint8_t targetId = buffer[TARGET_ID_INDEX];
  if (targetId != nodeId) {
    //P("Other target: %02x != %02x\r\n", targetId, nodeId);
    return 0;
  }
  
  // First check END_SENTINEL
  if (buffer[bufferInUse-1] != END_SENTINEL) {
    P("Bad end: %02x@%02x\r\n", buffer[bufferInUse-1], bufferInUse-1);
    return 0;
  }
  
  // Finally check the CRC
  uint8_t payloadSize = buffer[PAYLOAD_SIZE_INDEX];  
  uint8_t crcIndex = PAYLOAD_INDEX+payloadSize;
  
  uint32_t actualCrc = crc32(buffer, crcIndex);
  uint32_t *claimedCrc = (uint32_t *)(buffer + crcIndex);
  if (actualCrc != *claimedCrc) {
    P("Bad crc: %08lx != %08lx bytes 0+%x\r\n", actualCrc, *claimedCrc, crcIndex);
    return 0;
  } 
  
  uint8_t type = buffer[MESSAGE_TYPE_INDEX];
  uint8_t sourceId = buffer[SOURCE_ID_INDEX];
  P("Got healty message %02x -> %02x of type %02x with %02x bytes payload\r\n",
    sourceId, targetId, type, payloadSize);
  
  GPWRITE(RS485_LED, rxCount & 1);
  rxCount++;  
   
    
  if (type == 0x00) {
    uint8_t lastEvent = buffer[PAYLOAD_INDEX];
    return handlePoll(buffer, sourceId, targetId, lastEvent);
  } else {
    P("Error: Cannot handle message type %02x, sent from %02x, to %02x with %02x bytes payload\r\n",
      type, sourceId, targetId, payloadSize);    
    return 0;
  }  
}
