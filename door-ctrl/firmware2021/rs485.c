#include "rs485.h"
#include "board.h"
#include "avr8gpio.h"
#include <avr/interrupt.h>
#include "frame.h"
#include "aes256cbc.h"
#include "crc32.h"
#include "uart.h"

#if RS485_UART==0

#define UCSRnA UCSR0A
#define UCSRnB UCSR0B
#define UCSRnC UCSR0C
#define UDREn UDRE0
#define UDRn UDR0
#define FEn FE0
#define RXCn RXC0
#define DORn DOR0
#define RXENn RXEN0
#define TXENn TXEN0
#define U2Xn U2X0
#define UBRRnH UBRR0H
#define UBRRnL UBRR0L
#define TXCn TXC0
#define TXCIEn TXCIE0
#define UDRIEn UDRIE0
#define RXCIEn RXCIE0
#define UCSZn1 UCSZ01
#define UCSZn0 UCSZ00


#define UDRE_vect USART0_UDRE_vect
#define TX_vect USART0_TX_vect
#define RX_vect USART0_RX_vect

#elif RS485_UART==1

#define UCSRnA UCSR1A
#define UCSRnB UCSR1B
#define UCSRnC UCSR1C
#define UDREn UDRE1
#define UDRn UDR1
#define FEn FE1
#define RXCn RXC1
#define DORn DOR1
#define RXENn RXEN1
#define TXENn TXEN1
#define U2Xn U2X1
#define UBRRnH UBRR1H
#define UBRRnL UBRR1L
#define TXCn TXC1
#define TXCIEn TXCIE1
#define UDRIEn UDRIE1
#define RXCIEn RXCIE1
#define UCSZn1 UCSZ11
#define UCSZn0 UCSZ10

#define UDRE_vect USART1_UDRE_vect
#define TX_vect USART1_TX_vect
#define RX_vect USART1_RX_vect

#else
#error "Set RS485_UART"
#endif

// TODO: Initialize these from EEPROM:
uint8_t nodeId;
uint8_t nodeKey[AES_BLOCK_SIZE];

// This buffer is used for both transmit and receive
#define MAX_BUFFER 250
uint8_t buffer[MAX_BUFFER];
uint8_t bufferInUse = 0;
uint8_t bytesLeft = 0;

// Just a counter of received messages
uint16_t rxCount = 0;


enum CommState {
  CS_RX_IDLE, // Waiting for START_SENTINEL
  CS_RX, // Reading frame
  CS_TX // Buffer used to transmit
} commState;

void startRx(void) {
  commState = CS_RX_IDLE;
  bufferInUse = 0;
}

void handleReceivedBuffer(void) {
  uint8_t targetId = buffer[TARGET_ID_INDEX];
  if (targetId != nodeId) {
    //P("Other target: %02x != %02x\r\n", targetId, nodeId);
    return;
  }
  
  // First check END_SENTINEL
  if (buffer[bufferInUse-1] != END_SENTINEL) {
    P("Bad end: %02x@%02x\r\n", buffer[bufferInUse-1], bufferInUse-1);
    return;
  }
  
  uint8_t payloadSize = buffer[PAYLOAD_SIZE_INDEX];  
  uint8_t crcIndex = PAYLOAD_INDEX+payloadSize;
  
  uint32_t actualCrc = crc32(buffer, crcIndex);
  uint32_t *claimedCrc = (uint32_t *)(buffer + crcIndex);
  if (actualCrc != *claimedCrc) {
    P("Bad crc: %08lx != %08lx bytes 0+%x\r\n", actualCrc, *claimedCrc, crcIndex);
    return;
  } 
  
  // Find the handler of the specific message type
  uint8_t type = buffer[MESSAGE_TYPE_INDEX];
  P("Got healty message of type: %02x\r\n", type);
    
}

uint16_t rs485RxCount(void) {
  return rxCount;
}

// Transmit Data Register Empty interrupt
ISR(UDRE_vect) {
  // Feed more data to UDRn
}

// Transmit complete interrupt
ISR(TX_vect) {   
  // Release the RS485 bus
  GPCLEAR(RS485_TX_ENABLE);
}

// Receive complete Data Register Empty interrupt
ISR(RX_vect) {
  // Read data from URDn into the receive buffer  
  
  uint8_t rx = UDRn;
  if (commState == CS_RX_IDLE) {      
    if (rx == START_SENTINEL) {
      rxCount++;
      commState = CS_RX;
      bufferInUse = 0;
    }
  } else if (commState == CS_RX) {        
    if (bufferInUse >= MAX_BUFFER) {
      L("Overflow");
      startRx();
    }
    uint8_t index = bufferInUse++;
    buffer[index] = rx;
    P("%02x@%02x ", rx, index);
    
    if (index == PAYLOAD_SIZE_INDEX) {
      bytesLeft = rx + CRC32_SIZE + 1;
      
    } else if (index > PAYLOAD_SIZE_INDEX) {
      if (--bytesLeft == 0) {
        commState = CS_TX;
        handleReceivedBuffer();
        startRx();
      }
    }
  }
}

void rs485Init(void) {
  // TODO: Read nodeId and nodeKey from EEPROM
  nodeId = 0xff;
  
  startRx();
  
  GPOUTPUT(RS485_TX_ENABLE);
  GPOUTPUT(RS485_LED);
  GPCLEAR(RS485_TX_ENABLE);
  
  UCSRnC = _BV(UCSZn0) | _BV(UCSZn1); // 8N1
          
  UBRRnH = 0;
  UBRRnL = (F_CPU / (8UL * RS485_BAUD)) - 1;  
  UCSRnA = _BV(U2Xn);   
  UCSRnB = _BV(TXENn) // Transmit
          | _BV(RXENn) // Receive
          | _BV(TXCIEn) // Get interrupt when transmission is done
          | _BV(UDRIEn) // Get interrupt when buffer ready for more data
          | _BV(RXCIEn) // Get interrupt when data is received
          ;           
  sei();          
}
