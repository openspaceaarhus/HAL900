#pragma once

#include <stdint.h>

struct WiegandData {
  uint8_t bits;
  uint8_t bytes[7];
} WiegandData;

void initWiegand(void);
void pollWiegandTimeout(void);

