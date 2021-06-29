#include "gpio.h"
#include "random.h"
#include "events.h"
#include "uart.h"

uint32_t currentToken;
volatile uint16_t msTimer=1000;
volatile uint8_t gpioTimer=0;
volatile uint8_t stateAfterTimeout = 0;
volatile uint8_t timeoutPending = 0;

void refreshToken() {
  L("Set new token");
  randomBytes((uint8_t *)&currentToken, sizeof(currentToken));  
  controlTokenEvent(&currentToken);
}

void gpioInit(void) {
  L("gpio init");
  gpioTimer = 0;
  refreshToken();
}

void changeState(uint8_t newState) {
  controlStateEvent(newState);
  P("New state: %x\r\n", newState);
  // TODO: Actually control something
  
}

// Called once, when the timeout happens
void gpioPollTimeout(void) {
  if (timeoutPending) {
    timeoutPending = 0;
    changeState(stateAfterTimeout);  
  }
}

// Called at 1 kHz
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
     changeState(stateNow);
     msTimer = 1000;
     gpioTimer = timeout;
     stateAfterTimeout = stateLater;    
  }
  refreshToken();  
}
