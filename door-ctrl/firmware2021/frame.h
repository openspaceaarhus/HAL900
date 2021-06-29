#pragma once

#include <stdint.h>

#define START_SENTINEL 0xf0
#define END_SENTINEL 0xf1
#define CRC32_SIZE 4

#define MINIMUM_BYTES_IN_FRAME 10

// NOTE: These indexes are 1 less than the ones in Frame.java because we don't store the start sentinel.

#define SOURCE_ID_INDEX 0
#define TARGET_ID_INDEX 1
#define MESSAGE_TYPE_INDEX 2
#define PAYLOAD_SIZE_INDEX 3
#define PAYLOAD_INDEX 4

// Message types
#define MT_POLL 0x00
#define MT_ENROLL_REQ 0x01
#define MT_ENROLL_RESPONSE 0x02
#define MT_POLL_ACK 0x03
#define MT_POLL_RESPONSE 0x04
#define MT_OUTPUT 0x05


void frameInit(void);
uint8_t handleFrame(uint8_t* buffer, uint8_t bufferInUse);
uint16_t frameRxCount(void);
uint8_t getNodeId();
