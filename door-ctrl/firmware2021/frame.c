#include <string.h>

#include "frame.h"
#include "aes256cbc.h"
#include "crc32.h"
#include "uart.h"
#include "board.h"
#include "random.h"
#include "eeprom.h"
#include "events.h"
#include "rs485.h"

// Just a counter of received messages
uint16_t rxCount = 0;

uint8_t nodeId;
uint8_t nodeKey[AES_KEY_SIZE];
uint32_t enrollmentId = 0;

uint16_t frameRxCount(void) {
  return rxCount;
}

void frameInit(void) {
  eepromRead(&nodeId, nodeKey);
  GPOUTPUT(RS485_LED);
}

uint8_t createReply(uint8_t *buffer, uint8_t sourceId, uint8_t targetId, uint8_t type, uint8_t payloadSize) {
  
  buffer[SOURCE_ID_INDEX] = sourceId;
  buffer[TARGET_ID_INDEX] = targetId;
  buffer[MESSAGE_TYPE_INDEX] = type;
  buffer[PAYLOAD_SIZE_INDEX] = payloadSize;
  uint8_t crcIndex = PAYLOAD_INDEX+payloadSize;
  uint32_t actualCrc = crc32(buffer, crcIndex);
  *(uint32_t *)(buffer + crcIndex) = actualCrc;
  buffer[crcIndex+4] = END_SENTINEL;
  return crcIndex+5;
}

uint8_t createPollAck(uint8_t *buffer) {
  return createReply(buffer, nodeId, 0x00, MT_POLL_ACK, 0);
}

uint8_t createEnrollReq(uint8_t *buffer) {
  while (!enrollmentId) {
    randomBytes((uint8_t*)&enrollmentId, 4);
  }
  
  *(uint32_t*)(buffer + PAYLOAD_INDEX) = enrollmentId;
  P("Enrollment: %08lx\r\n", enrollmentId);  
  return createReply(buffer, 0xff, 0x00, MT_ENROLL_REQ, 4);
}

uint8_t handleEnrollResponse(uint8_t *buffer, uint8_t sourceId, uint8_t targetId) {  
  if (buffer[PAYLOAD_SIZE_INDEX] != 4+1+AES_KEY_SIZE) {
    P("Bad length of payload size: %d\r\n", buffer[PAYLOAD_SIZE_INDEX]);
    return 0;
  }
  
  if (memcmp(buffer + PAYLOAD_INDEX, &enrollmentId, 4)) {
    L("Bad enrollment id");
    return 0;
  }
    
  nodeId = buffer[PAYLOAD_INDEX + 4];
  memcpy(nodeKey, buffer + PAYLOAD_INDEX + 4+1, AES_KEY_SIZE);
  P("Enrolled: %01x\r\n", nodeId);
  
  eepromWrite(nodeId, nodeKey);
  
  return 0;
}

uint8_t encryptPayload(uint8_t *buffer, uint8_t rawBytes) {
  uint8_t *iv = buffer+PAYLOAD_INDEX;
  randomBytes(iv, AES_BLOCK_SIZE); // IV
  buffer[PAYLOAD_INDEX+AES_BLOCK_SIZE] = rawBytes; // Actual plain-text payload
  
  uint8_t * data=buffer+PAYLOAD_INDEX+16+1;
  uint32_t crc = crc32(data, rawBytes);
  memcpy(data+rawBytes, &crc, 4); 
  
  uint8_t paddedSize = rawBytes+4; // The padded size contains the crc32
  if (paddedSize & (AES_BLOCK_SIZE-1)) {
    paddedSize = ((paddedSize >> 4)+1)<<4;
  }
  
  aes256cbcInit(nodeKey, iv);
  uint8_t blockOffset = 0;
  while (blockOffset < paddedSize) {
    aes256cbcEncrypt(data+blockOffset);
    blockOffset += AES_BLOCK_SIZE;
  }
  
  return paddedSize+16+1;  
}

uint8_t createPollResponse(uint8_t *buffer, uint8_t sourceId) {
  uint8_t lastEvent = buffer[PAYLOAD_INDEX];
  // Before this point the buffer contains input, after it contains output.
  memset(buffer, 0xaa, MAX_BUFFER); // This makes it obvious if some parts aren't set
  uint8_t eventBytes = popEvents(lastEvent, buffer+PAYLOAD_INDEX+16+1, 200);
  uint8_t payloadSize = encryptPayload(buffer, eventBytes);
  
  return createReply(buffer, nodeId, sourceId, MT_POLL_RESPONSE, payloadSize);
}

uint8_t handlePoll(uint8_t *buffer, uint8_t sourceId, uint8_t targetId) {  
  if (nodeId == 0xff) {
    return createEnrollReq(buffer);
  } else {
    return createPollResponse(buffer, sourceId);
  }
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
  //P("Got healthy message %02x -> %02x of type %02x with %02x bytes payload\r\n", sourceId, targetId, type, payloadSize);
  
  GPWRITE(RS485_LED, rxCount & 1);
  rxCount++;  
   
    
  if (type == MT_POLL) {
    return handlePoll(buffer, sourceId, targetId);
    
  } else if (type == MT_ENROLL_RESPONSE) {
    return handleEnrollResponse(buffer, sourceId, targetId);
    
  } else {
    P("Error: Cannot handle message type %02x, sent from %02x, to %02x with %02x bytes payload\r\n",
      type, sourceId, targetId, payloadSize);    
    return 0;
  }  
}
