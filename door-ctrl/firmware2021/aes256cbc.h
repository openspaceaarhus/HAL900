#pragma once

#include "stdint.h"

// Size of a key, IV and block
#define AES_BLOCK_SIZE 16

void aes256cbcInit(const uint8_t key[AES_BLOCK_SIZE], const uint8_t initialization_vector[AES_BLOCK_SIZE]);
void aes256cbcEncrypt(uint8_t block[AES_BLOCK_SIZE]);
void aes256cbcDecrypt(uint8_t block[AES_BLOCK_SIZE]);
