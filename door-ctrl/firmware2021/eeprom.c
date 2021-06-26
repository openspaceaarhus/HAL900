#include <avr/eeprom.h>
#include <avr/interrupt.h>

#include "eeprom.h"
#include "aes256cbc.h"
#include "crc32.h"
#include "uart.h"
#include "string.h"

#define CONFIG_SIZE 1+AES_KEY_SIZE+4

void eepromWrite(uint8_t nodeId, uint8_t *nodeKey) {
  uint8_t config[CONFIG_SIZE];
  
  config[0] = nodeId;
  memcpy(config+1, nodeKey, AES_KEY_SIZE);
  uint32_t crc = crc32(config, 1+AES_KEY_SIZE);
  memcpy(config+1+AES_KEY_SIZE, &crc, 4);  
  /*
  P("Storing %d bytes (crc:%08lx) ", CONFIG_SIZE, crc);
  for (uint8_t i=0;i<CONFIG_SIZE;i++) {
    P("%02x ", config[i]);
  }
  L("");
  */
  cli();
  eeprom_write_block(config, 0, CONFIG_SIZE);  
  sei();
}

void eepromRead(uint8_t *nodeId, uint8_t *nodeKey) {
  uint8_t config[CONFIG_SIZE];
  
  cli();
  eeprom_read_block(config, 0, CONFIG_SIZE);  
  sei();
  /*
  P("Loading %d bytes ", CONFIG_SIZE);
  for (uint8_t i=0;i<CONFIG_SIZE;i++) {
    P("%02x ", config[i]);
  }
  L("");
  */
  uint32_t storedCrc;
  memcpy(&storedCrc, config+1+AES_KEY_SIZE, 4);

  uint32_t actualCrc = crc32(config, 1+AES_KEY_SIZE);
  if (actualCrc != storedCrc) {
    P("Bad EEPROM CRC: %08lx != %08lx\r\n", actualCrc, storedCrc);
    *nodeId = 0xff;
    return;
  }
  
  *nodeId = config[0];
  memcpy(nodeKey, config+1, AES_KEY_SIZE);
  
  P("Loaded config node=%02x\r\n", *nodeId); 
}

