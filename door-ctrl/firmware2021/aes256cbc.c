#include "aes256cbc.h"
#include "aes.h"
#include <string.h>

aes_256_context_t context;
uint8_t current_vector[16];

void aes256cbcInit(const uint8_t key[16], const uint8_t initialization_vector[16]) {
  // Initialise the context with the key
  aes_256_init(&context, key);

  // Copy the IV into the current vector array
  memcpy(current_vector, initialization_vector, 16);
}

void aes256cbcEncrypt(uint8_t block[16]) {
  // XOR the current vector with the block before encrypting
  for (uint8_t i = 0; i < 16; i++) {
    block[i] ^= current_vector[i];
  }

  // Encrypt the block
  aes_256_encrypt(&context, block);

  // Copy the cipher output to the current vector
  memcpy(current_vector, block, 16);
}

void aes256cbcDecrypt(uint8_t block[16]) {
  // Copy the cipher output to the temporary vector
  uint8_t temp_vector[16];
  memcpy(temp_vector, block, 16);

  // Decrypt the block
  aes_256_decrypt(&context, block);

  // XOR the output with the current vector to fully decrypt
  for (uint8_t i = 0; i < 16; i++) {
    block[i] ^= current_vector[i];
  }

  // Copy the temporary vector to the current vector
  memcpy(current_vector, temp_vector, 16);    
}
