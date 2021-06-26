#include <avr/interrupt.h>
#include <avr/io.h>
#include "wiegand.h"
#include "events.h"
#include "board.h"
#include "uart.h"

struct WiegandData data;
volatile unsigned char state;
volatile unsigned char timeout;

void initWiegand(void) {
  timeout = 0;

  // Enable pin change interrupt for the wiegand inputs, switch the led/beeper pins to output and off:
  PCMSK0 |= _BV(PCINT0) | _BV(PCINT1);
  GPINPUT(WIEGAND_D0);
  GPINPUT(WIEGAND_D1);
  GPOUTPUT(UNLOCK_LED);

  PCICR |= _BV(PCIE0);
  sei();

  data.bits = 0;
}


ISR(PCINT0_vect) {
  timeout = 0;
  uint8_t newState = (GPREAD(WIEGAND_D0) ? 1 : 0) | (GPREAD(WIEGAND_D1) ? 2 : 0);
  
  uint8_t rfidBit0 = (state & 1) && !(newState & 1);
  uint8_t rfidBit1 = (state & 2) && !(newState & 2);

    P("bit %d: %d %d\r\n", data.bits, rfidBit0, rfidBit1);
  if (rfidBit0 || rfidBit1) {    
    
    uint8_t byteIndex = data.bits >> 3;
    uint8_t bitValue  = 1<<(data.bits & 7);
    
    if (rfidBit1) {
      data.bytes[byteIndex] |= bitValue;
    } else {
      data.bytes[byteIndex] &=~ bitValue;
    }    
    data.bits++;
  }
  
  state = newState;
}

void pollWiegandTimeout() {
  if (timeout++ > 10) {        
    if (data.bits >= 4) {
      wiegandInput(&data);
      data.bits = 0;
    }
  }
}


