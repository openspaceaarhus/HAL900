#include "events.h"
#include "uart.h"
#include <string.h>

#define EV_POWER_UP 0
#define EV_WIEGAND 1
#define EV_CTRL_TOKEN 3
#define EV_MSG 5

#define MAX_EVENT_BYTES 500
uint8_t eventBuffer[MAX_EVENT_BYTES];
uint16_t eventBufferInUse = 0;
uint8_t nextEventNumber = 1;
uint8_t rebooted = 1;

void event(uint8_t type, uint8_t *data, uint8_t dataSize) {
  uint8_t eventSize = dataSize+3;
  
  if (MAX_EVENT_BYTES <= eventBufferInUse+eventSize) {
    P("Event buffer overflow dropping %02x with %02x payload\r\n", type, dataSize);    
    return;
  }

  eventBuffer[eventBufferInUse++] = type;
  eventBuffer[eventBufferInUse++] = nextEventNumber++;
  eventBuffer[eventBufferInUse++] = dataSize;
  memcpy(eventBuffer+eventBufferInUse, data, dataSize);
  eventBufferInUse += dataSize;  
  if (!nextEventNumber) {
    nextEventNumber = 1;
  }
  
  //P("EB: %02x %d %d\r\n", type, dataSize, eventBufferInUse);
}

void powerUpEvent(void) {
  rebooted = 1;
  event(EV_POWER_UP, 0, 0);  
}

void wiegandInput(struct WiegandData *data) {
  event(EV_WIEGAND, (void *)data, sizeof(WiegandData));   
}

// Skips events older than skipOlder and removes them from the buffer,
// then copies out at most maxBytes of events from the buffer
// and returns the number of event bytes actually returned.
uint8_t popEvents(uint8_t skipOlder, uint8_t *buffer, uint8_t maxBytes) {
  // First remove the events older than skipOlder
  //P("EB iu:%d rb:%d so:%d nn:%d\r\n", eventBufferInUse, rebooted, skipOlder, nextEventNumber);
  
  if (rebooted) {    
    if (skipOlder < nextEventNumber) {
      rebooted = 0;      
    }
  } else {
    uint16_t pos = 0;
    while (pos < eventBufferInUse) {
      uint8_t number = eventBuffer[pos+1];
      if ((skipOlder < 128 && (number > 128 || number <= skipOlder)) ||
          (skipOlder >= 128 && number <= skipOlder)) {
        // Needs to be skipped
        //P("Skipping %d <= %d\r\n", number, skipOlder);
      } else {
        break;
      }
      uint8_t dataSize = eventBuffer[pos+2];
      pos += dataSize+3;
    }
    if (pos) {
      uint16_t newInUse = eventBufferInUse-pos;
      memcpy(eventBuffer, eventBuffer+pos, newInUse);
      eventBufferInUse = newInUse;
    }
  }
  
  // The simple case where it fits,
  // this means that we don't have to parse through the events,
  // we just fire them all off:
  if (eventBufferInUse < maxBytes) {
    memcpy(buffer, eventBuffer, eventBufferInUse);
    return eventBufferInUse;
  }
    
  uint16_t pos = 0;
  while (pos < eventBufferInUse) {
    uint8_t dataSize = eventBuffer[pos+2];
    uint16_t bytesWithThisEvent = pos + dataSize+3;
    if (maxBytes < bytesWithThisEvent) {
      memcpy(buffer, eventBuffer, pos);
      return pos;
    }
    pos = bytesWithThisEvent;
  }
  
  return 0;
  
}
