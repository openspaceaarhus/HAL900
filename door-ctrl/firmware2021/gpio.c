#include "gpio.h"
#include "random.h"
#include "events.h"
#include "uart.h"
#include "board.h"
#include "leds.h"

uint8_t stableState;
uint8_t stableTime;
uint8_t lastReportedState;
uint32_t currentToken;
volatile uint16_t msTimer=1000;
volatile uint8_t gpioTimer=0;
volatile uint8_t stateAfterTimeout = 0;
volatile uint8_t timeoutPending = 0;

void refreshToken() {
  if (currentToken) {
    currentToken++;
  } else {
    randomBytes((uint8_t *)&currentToken, sizeof(currentToken));  
  }
  controlTokenEvent(&currentToken);
  //P("New token: %lx\r\n", currentToken);
}

uint8_t currentState(void) {
  return
  (GPREAD(RELAY1)     ? 0x01 : 0) |
  (GPREAD(RELAY2)     ? 0x02 : 0) |
  (GPREAD(UNLOCK_LED) ? 0x04 : 0) |
  (GPREAD(EXTRA_OUT)  ? 0x08 : 0) |

  (GPREAD(PORT_3_1) ? 0x10 : 0) |
  (GPREAD(PORT_3_2) ? 0x20 : 0) |
  (GPREAD(PORT_3_3) ? 0x40 : 0) |
  (GPREAD(PORT_3_6) ? 0x80 : 0);
}

void gpioInit(void) {
  gpioTimer = 0;
  
  GPOUTPUT(RELAY1);
  GPOUTPUT(RELAY2);
  GPOUTPUT(UNLOCK_LED);
  GPOUTPUT(EXTRA_OUT);
  
  GPINPUT(PORT_3_1);
  GPINPUT(PORT_3_2);
  GPINPUT(PORT_3_3);
  GPINPUT(PORT_3_6);
  GPSET(PORT_3_1);
  GPSET(PORT_3_2);
  GPSET(PORT_3_3);
  GPSET(PORT_3_6);
  
  refreshToken();
  controlStateEvent(currentState());
}

void reportStateIfChanged() {
  uint8_t newState = currentState();
  if (newState != stableState) {
    stableTime = 0;
    stableState = newState;
  } else {
    if (stableTime < 100) {
      stableTime++;
    } else {
      if (lastReportedState != stableState) {
	lastReportedState = stableState;
	controlStateEvent(lastReportedState);
	//P("New state: %x -> %x\r\n", lastReportedState, newState);
	setLEDs(lastReportedState>>2);
      }
    }
  }
}

void changeState(uint8_t newState) {
  
  GPWRITE(RELAY1, newState & 1);
  GPWRITE(RELAY2, newState & 2);
  GPWRITE(UNLOCK_LED, newState & 4);
  GPWRITE(EXTRA_OUT, newState & 8);
  //P("Set state: %x got %x\r\n", newState, currentState());
  
  reportStateIfChanged();
}

// Called once, when the timeout happens
void gpioPollTimeout(void) {
  if (timeoutPending) {
    timeoutPending = 0;
    changeState(stateAfterTimeout);  
  }
  reportStateIfChanged();
}

// Called at 1 kHz, don't do anything here that can be done elsewhere.
void gpioTick(void) {  
  if (!--msTimer) {
    msTimer = 1000;
    if (gpioTimer && !--gpioTimer) {
      timeoutPending = 1;
    }
  }
}

void gpioSet(uint32_t token, uint8_t stateNow, uint8_t timeout, uint8_t stateLater) {
  if (token == currentToken) {
    //P("Good token: %lx\r\n", token);
    changeState(stateNow);
    msTimer = 1000;
    gpioTimer = timeout;
    stateAfterTimeout = stateLater; 
  } else {
    //P("Bad token: %lx != %lx\r\n", token, currentToken);
  }
  refreshToken();  
}
