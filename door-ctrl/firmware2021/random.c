#include "random.h"
#include "board.h"
#include "uart.h"
#include <avr/io.h>
#include <avr/interrupt.h>

// The number of samples to pull from the adc for each time we want to refill the entropy pool
#define REFEED_CYCLES 100
#define ENTROPY_SIZE 33
uint8_t entropy[ENTROPY_SIZE];
uint8_t refeed;
uint8_t put = 0;
uint8_t take = 0;

ISR(ADC_vect) {
  uint8_t adcl = ADCL;  
  uint8_t adcWrapped = ADCH ^ adcl;
  // We smush all the 10 active bits into a single byte
  // the distribution of randomness is skewed to the lower ~7 bits of the 10 bits sampled,
  // with a 750 mVpp noise signal on the input
  
  // Now we smear the newly obtained low-quality entropy all over the entropy store
  // we rotate the bits in adcWrapped so we end up changing all the bits over time  
  for (int i=0;i<ENTROPY_SIZE;i++) {
    entropy[put++] ^= adcWrapped;
    if (put >= ENTROPY_SIZE) {
      put = 0;
    }
    
    if (adcWrapped & 1) {
      adcWrapped = ((adcWrapped >> 1) | 128);
    } else {
      adcWrapped = adcWrapped >> 1;
    }
  }
  
  if (--refeed) {
    ADCSRA |= _BV(ADSC);
  }
}

void dumpEntropy(void) {  
  for (int i=0;i<ENTROPY_SIZE;i++) {
    P(" %02x", randomByte());
  }
}

void randomInit(void) {
  ADMUX = NOISE_ADC | _BV(REFS0) | _BV(ADLAR);
  ADCSRA = _BV(ADEN) | _BV(ADSC) | _BV(ADIE) 
           | _BV(ADPS0) | _BV(ADPS1) | _BV(ADPS2);
  ADCSRB = 0;
  DIDR0 = _BV(NOISE_ADC);
  refeed = REFEED_CYCLES;
}

uint8_t randomByte(void) {
//  while (refeed) {} // Wait for refeeding to finish
  uint8_t rnd = entropy[take++];
  if (take >= ENTROPY_SIZE) {
    take = 0;
    refeed = REFEED_CYCLES;
    ADCSRA |= _BV(ADSC);    
  }
  
  if (take == ENTROPY_SIZE/2) {
    refeed = REFEED_CYCLES;
    ADCSRA |= _BV(ADSC);
  }
  
  return rnd;
}

void randomBytes(uint8_t *target, uint8_t size) {
  for (int i=0;i<size;i++) {
      *target++ = randomByte();
  }  
}

