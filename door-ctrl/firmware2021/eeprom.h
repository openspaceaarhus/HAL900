#pragma once

#include <stdint.h>

void eepromWrite(uint8_t nodeId, uint8_t *nodeKey);
void eepromRead(uint8_t *nodeId, uint8_t *nodeKey);
