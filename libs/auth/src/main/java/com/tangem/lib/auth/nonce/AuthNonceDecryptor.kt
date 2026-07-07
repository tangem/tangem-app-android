package com.tangem.lib.auth.nonce

/**
 * Decrypts server-issued nonces
 */
interface AuthNonceDecryptor {

    /**
     * Decrypts [encryptedNonce] — a Base64url-encoded (no padding) RSA-encrypted nonce from the backend.
     *
     * @param encryptedNonce Base64url-encoded encrypted nonce
     * @return decrypted nonce as a string
     * @throws Exception if decryption fails (invalid key, corrupted ciphertext, etc.)
     */
    suspend fun decryptNonce(encryptedNonce: String): String
}