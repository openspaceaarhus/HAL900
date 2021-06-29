#pragma once

#include "avr8gpio.h"

// Interface to the keypad
#define WIEGAND_D0  GPA0
#define WIEGAND_D1  GPA1
#define UNLOCK_LED  GPA2
#define EXTRA_OUT   GPA3

// RS485 on UART 0
#define RS485_UART 0
#define RS485_BAUD 19200
#define RS485_LED   GPC2
#define RS485_TX_ENABLE   GPD7

// charlieplex of status LEDs
#define LED1 GPC5
#define LED2 GPC4
#define LED3 GPC3

// Relay outputs
#define RELAY1 GPC7
#define RELAY2 GPC6

#define PORT_2_1 GPA4
#define PORT_2_2 GPA5
#define PORT_2_3 GPA6
#define PORT_2_6 GPA7

#define PORT_3_1 GPB3
#define PORT_3_2 GPD6
#define PORT_3_3 GPD5
#define PORT_3_6 GPD4

// PA4 is used for noise inputs
#define NOISE_ADC 4

#define DEBUG1 GPB4
#define DEBUG2 GPB2
