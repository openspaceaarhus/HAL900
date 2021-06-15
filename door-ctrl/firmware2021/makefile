PROJ=door21

include ../atmega8/makefile

AVR_PROGRAMMER    = -c atmelice_isp -P usb
AVR               = atmega324p
UART_BAUD         = 115200
F_CPU             = 20000000

# Need to slow down the programming because we're running on 1 MHz internal RC clock
PROGRAM_BIT_TIME  = 1

readfuses:
	avrdude $(AVR_PROGRAMMER) -p $(AVR) -B 100 -U "lfuse:r:-:h" -U "hfuse:r:-:h"
# (E:FC, H:D9, L:CE)

fuses:
	avrdude $(AVR_PROGRAMMER) -p $(AVR) -B 100 -U "lfuse:w:0xce:m" -U "hfuse:w:0xd9:m"
