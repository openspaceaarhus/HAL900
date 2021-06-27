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
#include "leds.h"
#include "wiegand.h"
   
int main(void) {
  wdt_enable(WDTO_4S);  
  uartInit();
  L("Booting");
  randomInit();
  frameInit();
  rs485Init();
  initWiegand();
  initLEDs();
  powerUpEvent();
  
  uint16_t lastRxCount = 0;
  while (1) {
    wdt_reset();
    uint16_t thisRx = frameRxCount();
    if (thisRx != lastRxCount) {
      setLEDs(thisRx);
      //P("Frames: %d\r\n", frameRxCount());
      lastRxCount = thisRx;
//      set_sleep_mode(0);
      //sleep_cpu();
    }
    //msgEvent("Frames: %d", frameRxCount());
    
    PORTA = thisRx & 3;
  }
}
