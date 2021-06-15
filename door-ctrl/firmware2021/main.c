#include <avr/wdt.h> 
#include <stdio.h>
#include <util/delay.h>

#include "board.h"

#include "uart.h"

#include "crc32.h"
#include "aes256cbc.h"

#include "board.h"
#include "rs485.h"

   
int main(void) {
  wdt_enable(WDTO_4S);
  uartInit();
  L("Booting");
  rs485Init();
  
  
/*
  aes256cbcInit(AES_KEY, IV);
  GPOUTPUT(GPB1);
    char *data = "0123456789abcdef";

    GPSET(GPB1);    
    aes256cbcEncrypt((uint8_t *)data);    
    GPCLEAR(GPB1);
  
  */
  
  while (1) {
    wdt_reset();
    _delay_ms(100);
    P("Frames: %d\r\n", rs485RxCount());
  }
}
