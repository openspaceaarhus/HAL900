#include <avr/wdt.h> 
#include <stdio.h>
#include <util/delay.h>

#include "board.h"

#include "uart.h"

#include "crc32.h"
#include "aes256cbc.h"

#include "board.h"
#include "rs485.h"
#include "frame.h"
#include "random.h"
#include "events.h"
   
int main(void) {
  wdt_enable(WDTO_4S);  
  uartInit();
  L("Booting");
  frameInit();
  rs485Init();
  randomInit();
  powerUpEvent();
  
  uint16_t lastRxCount = 0;
  while (1) {
    wdt_reset();
    _delay_ms(1000);
    uint16_t thisRx = frameRxCount();
    if (thisRx != lastRxCount) {
      //P("Frames: %d\r\n", frameRxCount());
      lastRxCount = thisRx;
    }
    //msgEvent("Frames: %d", frameRxCount());
  }
}
