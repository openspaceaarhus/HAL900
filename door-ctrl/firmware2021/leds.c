#include <avr/interrupt.h>
#include <avr/io.h>
#include <avr/sleep.h>

#include "leds.h"
#include "wiegand.h"
#include "board.h"
#include "gpio.h"

volatile uint8_t ledsOn;
volatile uint8_t plexCount;
volatile uint8_t ledDecimator;
volatile uint8_t ms;

void initLEDs(void) {
  ledsOn = 0;
  plexCount = 0;
  TCCR2B = 0; // Stop timer    
  
  TCCR2A = _BV(WGM21);
  TCNT2=0; 
  TIMSK2 = _BV(OCIE2A); // Fire interrupt when done

  // Set the timer to hit the interrupt at 1000 Hz or thereabouts  
  OCR2A=77; 
  TCCR2B = _BV(CS22) | _BV(CS21);

  sei(); // Enable interrupts!  
  sleep_enable();
}

const char PLEX[6][2] = {
  { _BV(PC4) | _BV(PC5), _BV(PC4) },
  { _BV(PC4) | _BV(PC5), _BV(PC5) },

  { _BV(PC3) | _BV(PC4), _BV(PC3) },
  { _BV(PC3) | _BV(PC4), _BV(PC4) },

  { _BV(PC3) | _BV(PC5), _BV(PC3) },
  { _BV(PC3) | _BV(PC5), _BV(PC5) },
};


ISR(TIMER2_COMPA_vect) {
  ms++;
    
  // Tristate all pins:
  DDRC  &=~ (_BV(PC3) | _BV(PC4) | _BV(PC5));
  PORTC &=~ (_BV(PC3) | _BV(PC4) | _BV(PC5));

  // Turn on the led we want:
  if (ledsOn & _BV(plexCount)) {
    DDRC  |= PLEX[plexCount][0];
    PORTC |= PLEX[plexCount][1];
  } 

  if (plexCount++ > 5) {
    plexCount = 0;
  }

  pollWiegandTimeout();  
  gpioTick();
}

void resetMsTimer(void) {
  ms = 0;  
}

void sleepUntilMs(uint8_t target) {
  while (ms < target) {
    set_sleep_mode(0);
    sleep_cpu();
  }
}

void setLED(unsigned char led, unsigned char on) {
  if (on) {
    ledsOn |= _BV(led);
  } else {
    ledsOn &=~ _BV(led);
  }
}

void setLEDs(unsigned char led) {
  ledsOn = led;
}
