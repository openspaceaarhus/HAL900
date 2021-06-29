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
#include "gpio.h"
   
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
  gpioInit();
  
  uint16_t lastRxCount = 0;
  while (1) {
    handleReceivedBuffer(); // Puts CPU to sleep if no work to be done.
    gpioPollTimeout();
    
    uint16_t thisRx = frameRxCount();
    if (thisRx != lastRxCount) {
      wdt_reset(); // This means that we'll reset if the controller ever goes down.
      setLEDs(thisRx);
      //P("Frames: %d\r\n", frameRxCount());
      lastRxCount = thisRx;
    }
    
    PORTA = thisRx & 3;
  }
}
