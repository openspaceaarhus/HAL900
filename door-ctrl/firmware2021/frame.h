#pragma once

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
