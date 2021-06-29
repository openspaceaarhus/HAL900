#pragma once

#include <stdint.h>

void gpioInit(void);
void gpioTick(void);
void gpioSet(uint32_t token, uint8_t stateNow, uint8_t timeout, uint8_t stateLater);
void gpioPollTimeout(void);
