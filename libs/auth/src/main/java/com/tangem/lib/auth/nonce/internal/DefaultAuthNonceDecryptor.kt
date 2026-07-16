package com.tangem.lib.auth.nonce.internal

import android.util.Base64
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

internal class DefaultAuthNonceDecryptor(
    authServiceKeyBase64: String,
    private val dispatchers: CoroutineDispatcherProvider,
) : AuthNonceDecryptor {

    private val privateKey = run {
        val keyBytes = Base64.decode(authServiceKeyBase64, Base64.NO_WRAP)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(keySpec)
    }

    override suspend fun decryptNonce(encryptedNonce: String): String = withContext(dispatchers.default) {
        try {
            val encryptedBytes = Base64.decode(encryptedNonce, Base64.URL_SAFE or Base64.NO_PADDING)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAM_SPEC)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            TangemLogger.e("Nonce decryption failed", e)
            throw e
        }
    }

    private companion object {
        const val KEY_ALGORITHM = "RSA"
        const val CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

        val OAEP_PARAM_SPEC = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT,
        )
    }
}