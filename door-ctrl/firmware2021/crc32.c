#include "crc32.h"
#include "uart.h"

// CCITT CRC-32 (Autodin II) polynomial:
// X32+X26+X23+X22+X16+X12+X11+X10+X8+X7+X5+X4+X2+X+1

uint32_t crc32(uint8_t *buffer, uint8_t length) {
  uint32_t crc = 0xffffffff;
  for (uint8_t i=0; i<length; i++) {
    crc ^= *buffer++;
    for (uint8_t j=0; j<8; j++) {
      if (crc & 1) {
	crc = (crc>>1) ^ 0xEDB88320;
      } else {
	crc = crc >>1;
      }
    }
  }
  return crc ^ 0xffffffff;
}


