#include <avr/interrupt.h>
#include <avr/io.h>
#include <string.h>

#include "wiegand.h"
#include "events.h"
#include "board.h"
#include "uart.h"

struct WiegandData data;
volatile unsigned char state = 0;
volatile unsigned char timeout;

void resetData(void) {
  memset(&data, 0, sizeof(data));  
}

void initWiegand(void) {
  timeout = 0;
  resetData();

  // Enable pin change interrupt for the wiegand inputs, switch the led/beeper pins to output and off:
  PCMSK0 = _BV(PCINT0) | _BV(PCINT1);
  GPINPUT(WIEGAND_D0);
  GPINPUT(WIEGAND_D1);
  GPOUTPUT(UNLOCK_LED);

  PCICR = _BV(PCIE0);
  sei();
}


ISR(PCINT0_vect) {
  timeout = 0;
  // wiegand is idle high, when a wire is pulsed low it counts as either a 0 or a 1
  uint8_t newState = (GPREAD(WIEGAND_D0) ? 0 : 1) | (GPREAD(WIEGAND_D1) ? 0 : 2);
  
  uint8_t rfidBit0 = (state & 1) && !(newState & 1);
  uint8_t rfidBit1 = (state & 2) && !(newState & 2);

  if (rfidBit0 || rfidBit1) {    
    uint8_t byteIndex = data.bits >> 3;
    data.bytes[byteIndex] <<= 1;
    if (rfidBit0) {
      data.bytes[byteIndex] |= 1;
    } 
      
    data.bits++;    
  }
  
  state = newState;
}

void pollWiegandTimeout() {
  if (timeout++ > 50) {        
    if (data.bits >= 4) {
      wiegandInput(&data);
      resetData();
    }
  }
}


