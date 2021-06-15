#pragma once

#include "stdint.h"

void aes256cbcInit(const uint8_t key[16], const uint8_t initialization_vector[16]);
void aes256cbcEncrypt(uint8_t block[16]);
void aes256cbcDecrypt(uint8_t block[16]);
