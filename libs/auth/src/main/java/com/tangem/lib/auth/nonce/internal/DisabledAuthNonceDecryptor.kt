package com.tangem.lib.auth.nonce.internal

import com.tangem.lib.auth.nonce.AuthNonceDecryptor

internal object DisabledAuthNonceDecryptor : AuthNonceDecryptor {

    override suspend fun decryptNonce(encryptedNonce: String): String =
        error("AuthNonceDecryptor is disabled: feature toggle is off or auth service key is missing")
}