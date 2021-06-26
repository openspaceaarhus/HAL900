#pragma once

#include <stdint.h>
#include "wiegand.h"

void event(uint8_t type, uint8_t *data, uint8_t dataSize);
void powerUpEvent(void);
void wiegandInput(struct WiegandData *data);

uint8_t popEvents(uint8_t skipOlder, uint8_t *buffer, uint8_t maxBytes);


