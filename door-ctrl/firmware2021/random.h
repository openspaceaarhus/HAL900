#pragma once
#include <stdint.h>

void randomInit(void);
void dumpEntropy(void);
uint8_t randomByte(void);
void randomBytes(uint8_t *target, uint8_t size);
