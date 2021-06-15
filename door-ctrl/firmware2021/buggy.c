#include <avr/interrupt.h>
#include "uart.h"

// Install all the interrupt vectors that are not actually in use

ISR(__vector_default) { L("Unhandled interrupt"); }

ISR(INT0_vect) { L("Caught External Interrupt Request 0"); }
ISR(INT1_vect) { L("Caught External Interrupt Request 1"); }
ISR(INT2_vect) { L("Caught External Interrupt Request 2"); }
//ISR(PCINT0_vect) { L("Caught Pin Change Interrupt Request 0"); }
ISR(WDT_vect) { L("Caught Watchdog Time-out Interrupt"); }
ISR(TIMER1_CAPT_vect) { L("Caught Timer/Counter1 Capture Event"); }
ISR(TIMER1_COMPA_vect) { L("Caught Timer/Counter1 Compare Match A"); }
ISR(TIMER1_COMPB_vect) { L("Caught Timer/Counter1 Compare Match B"); }
ISR(TIMER1_OVF_vect) { L("Caught Timer/Counter1 Overflow"); }
ISR(TIMER0_COMPA_vect) { L("Caught Timer/Counter0 Compare Match A"); }
ISR(TIMER0_COMPB_vect) { L("Caught Timer/Counter0 Compare Match B"); }
ISR(TIMER0_OVF_vect) { L("Caught Timer/Counter0 Overflow"); }
ISR(SPI_STC_vect) { L("Caught SPI Serial Transfer Complete"); }
//ISR(USART1_UDRE_vect) { L("Caught USART1 Data register Empty"); }
//ISR(USART1_TX_vect) { L("Caught USART1, Tx Complete"); }
ISR(ANALOG_COMP_vect) { L("Caught Analog Comparator"); }
ISR(ADC_vect) { L("Caught ADC Conversion Complete"); }
ISR(EE_READY_vect) { L("Caught EEPROM Ready"); }
ISR(TWI_vect) { L("Caught 2-wire Serial Interface"); }
ISR(SPM_READY_vect) { L("Caught Store Program Memory Read"); }
