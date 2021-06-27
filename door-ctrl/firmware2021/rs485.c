#include "rs485.h"

#include <avr/interrupt.h>
#include <util/delay.h>

#include "board.h"
#include "avr8gpio.h"
#include "frame.h"
#include "uart.h"
#include "leds.h"

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

// This buffer is used for both transmit and receive

uint8_t buffer[MAX_BUFFER];
uint8_t bufferInUse = 0;
uint8_t bytesLeft = 0;
uint8_t *currentTx;

enum CommState {
  CS_RX_IDLE, // Waiting for START_SENTINEL
  CS_RX, // Reading frame
  CS_WORKING, // Handling recevied buffer
  CS_TX, // Buffer used to transmit
  CS_TX_WAITING // Waiting for the last byte to complete transmission
} commState;

void startRx(void) {
  GPCLEAR(RS485_TX_ENABLE);
  commState = CS_RX_IDLE;
  bufferInUse = 0;
}

void handleReceivedBuffer(void) {
  // TODO: Use a timer to ensure consistent timing from
  // end of poll frame to start of response.
  resetMsTimer();
  
  uint8_t responseSize = handleFrame(buffer, bufferInUse);

  if (responseSize > 0) {
    //sleepUntilMs(5);
    _delay_ms(5);
    bytesLeft = responseSize;
    commState = CS_TX;
    /*
    P("Response %d bytes: \r\n", responseSize+1, buffer[MESSAGE_TYPE_INDEX]);
    for (int i=0; i<responseSize;i++) {
      P("%02x ", buffer[i]);
    }
    L("");
    */
    currentTx = buffer;
    GPSET(RS485_TX_ENABLE);    
    _delay_ms(1);
    UCSRnB |= _BV(UDRIEn); // Get interrupt when buffer ready for more data
    
    UDRn = START_SENTINEL;
  } else {
    startRx();
  }       
}


// Transmit Data Register Empty interrupt
ISR(UDRE_vect) {
  if (commState == CS_TX) {
    if (bytesLeft > 0) {
      uint8_t tx = *currentTx++;
//      P("TX: %02x@%02x\r\n", tx, bytesLeft);
      UDRn = tx;
      bytesLeft--;
      if (!bytesLeft) {
        UCSRnB &= ~_BV(UDRIEn); // Stop this interrupt
        commState = CS_TX_WAITING;
      }
    }
  }
}

// Transmit complete interrupt
ISR(TX_vect) {   
  if (commState == CS_TX_WAITING) {
    startRx(); // Go back to listening for a start sentinel  
  }
}

// Receive complete Data Register Empty interrupt
ISR(RX_vect) {
  // Read data from URDn into the receive buffer  
  
  uint8_t rx = UDRn;
  if (commState == CS_RX_IDLE) {      
    if (rx == START_SENTINEL) {
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
//    P("%02x@%02x ", rx, index);
    
    if (index == PAYLOAD_SIZE_INDEX) {
      bytesLeft = rx + CRC32_SIZE + 1;
      
    } else if (index > PAYLOAD_SIZE_INDEX) {
      if (--bytesLeft == 0) {
        commState = CS_WORKING;
        handleReceivedBuffer();
      }
    }
  }
}

void rs485Init(void) {
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
          | _BV(RXCIEn) // Get interrupt when data is received
          ;           
  sei();          
}
