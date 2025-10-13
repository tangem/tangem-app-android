package com.tangem.data.pay.util

import android.util.Base64
import com.tangem.utils.extensions.hexToBytes
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import com.tangem.common.extensions.toHexString
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

private const val KEY_LENGTH_BYTES = 16
private const val TAG_LENGTH_BYTES = 16
private const val BITS_PER_BYTE = 8
private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
private const val AES_ALGORITHM = "AES"
private const val RSA_ALGORITHM = "RSA"

internal class RainCryptoUtil @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun generateSecretKeyAndSessionId(publicKeyBase64: String): Pair<String, String> =
        withContext(dispatchers.default) {
            val secretKeyHex = ByteArray(KEY_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }.toHexString()
            val sessionId = generateSessionId(publicKeyBase64, secretKeyHex)
            secretKeyHex to sessionId
        }

    suspend fun decryptSecret(base64Secret: String, base64Iv: String, secretKeyHex: String): String =
        withContext(dispatchers.default) {
            val cipherTextBytes = Base64.decode(base64Secret, Base64.NO_WRAP)
            if (cipherTextBytes.size < TAG_LENGTH_BYTES) error("Cipher text too short")

            val initializationVectorBytes = Base64.decode(base64Iv, Base64.NO_WRAP)
            val secretKeyBytes = secretKeyHex.hexToBytes()

            val aesSecretKey = SecretKeySpec(secretKeyBytes, AES_ALGORITHM)
            val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH_BYTES * BITS_PER_BYTE, initializationVectorBytes)

            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.DECRYPT_MODE, aesSecretKey, gcmParameterSpec)

            val plaintextBytes = aesCipher.doFinal(cipherTextBytes)
            plaintextBytes.toString(StandardCharsets.UTF_8).trim().ifEmpty { error("Invalid decrypted data") }
        }

    private fun generateSessionId(publicKeyBase64: String, secretKeyHex: String): String {
        val secretKeyBytes = secretKeyHex.hexToBytes()
        val publicKeyDerBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
        val publicKeySpec = X509EncodedKeySpec(publicKeyDerBytes)
        val rsaPublicKey = KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(publicKeySpec)

        val secretKeyBase64String = Base64.encodeToString(secretKeyBytes, Base64.NO_WRAP)
        val plaintextUtf8Bytes = secretKeyBase64String.toByteArray(StandardCharsets.UTF_8)

        val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey)
        val cipherTextBytes = rsaCipher.doFinal(plaintextUtf8Bytes)

        return Base64.encodeToString(cipherTextBytes, Base64.NO_WRAP)
    }
}