package com.tangem.data.pay.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import com.tangem.data.pay.entity.EncryptedData
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
private const val IV_LENGTH_BYTES = 16
private const val PIN_LENGTH = 4
private const val PIN_LENGTH_BYTES = 8
private const val PIN_BLOCK_ISO_9564_FORMAT_PREFIX = '2'
private const val PIN_BLOCK_FILL_CHAR = 'F'

internal class RainCryptoUtil @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun generateSecretKeyAndSessionId(publicKeyBase64: String): Pair<ByteArray, String> =
        withContext(dispatchers.default) {
            val secretKeyBytes = ByteArray(KEY_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
            val sessionId = generateSessionId(publicKeyBase64, secretKeyBytes)
            secretKeyBytes to sessionId
        }

    suspend fun decryptPin(base64Secret: String, base64Iv: String, secretKeyBytes: ByteArray): String? {
        val pinBlock = decryptSecret(
            base64Secret = base64Secret,
            base64Iv = base64Iv,
            secretKeyBytes = secretKeyBytes,
        )
        return extractPinFromPinBlock(pinBlock)
    }

    suspend fun encryptPin(pin: String, secretKeyBytes: ByteArray): EncryptedData = withContext(dispatchers.default) {
        val bytes = pinBlockByteArray(pin)
        try {
            encryptSecret(bytes, secretKeyBytes)
        } finally {
            bytes.clear()
        }
    }

    suspend fun decryptSecret(base64Secret: String, base64Iv: String, secretKeyBytes: ByteArray): String =
        withContext(dispatchers.default) {
            val cipherTextBytes = Base64.decode(base64Secret, Base64.NO_WRAP)
            if (cipherTextBytes.size < TAG_LENGTH_BYTES) error("Cipher text too short")

            val initializationVectorBytes = Base64.decode(base64Iv, Base64.NO_WRAP)
            val aesSecretKey = SecretKeySpec(secretKeyBytes, AES_ALGORITHM)
            val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH_BYTES * BITS_PER_BYTE, initializationVectorBytes)

            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.DECRYPT_MODE, aesSecretKey, gcmParameterSpec)

            val plaintextBytes = aesCipher.doFinal(cipherTextBytes)
            plaintextBytes.toString(StandardCharsets.UTF_8).trim().ifEmpty { error("Invalid decrypted data") }
        }

    private fun encryptSecret(bytes: ByteArray, secretKeyBytes: ByteArray): EncryptedData {
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }

        val aesSecretKey = SecretKeySpec(secretKeyBytes, AES_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BYTES * BITS_PER_BYTE, iv)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesSecretKey, gcmSpec)

        val ciphertext = cipher.doFinal(bytes)
        bytes.clear()

        return EncryptedData(
            encryptedBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
        )
    }

    private fun generateSessionId(publicKeyBase64: String, secretKeyBytes: ByteArray): String {
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

    /**
     * Formats PIN into a PIN block using schema: [Prefix][Length][PIN][fill with F].
     * Example: 246784FFFFFFFFFF for PIN 6784.
     */
    private fun pinBlockByteArray(pin: String): ByteArray {
        require(pin.length == PIN_LENGTH) { "PIN length must be $PIN_LENGTH" }
        require(pin.all { it.isDigit() }) { "PIN must contain digits only" }

        val pinBlockHexLength = PIN_LENGTH_BYTES * 2
        val hex = buildString(pinBlockHexLength) {
            append(PIN_BLOCK_ISO_9564_FORMAT_PREFIX)
            append(PIN_LENGTH.toString())
            append(pin)
            while (length < pinBlockHexLength) append(PIN_BLOCK_FILL_CHAR)
        }
        return hex.toByteArray(StandardCharsets.UTF_8)
    }

    private suspend fun extractPinFromPinBlock(pinBlock: String): String? = withContext(dispatchers.default) {
        val pinLength = pinBlock[1].digitToIntOrNull()
        require(pinLength == PIN_LENGTH) { "Unexpected PIN length: ${pinBlock[1]}" }

        val pinStartIndex = 2
        val pinEndIndex = pinStartIndex + pinLength
        val pin = pinBlock.substring(pinStartIndex, pinEndIndex)

        pin.takeIf { value -> value.all { it.isDigit() } && value.length == PIN_LENGTH }
    }

    private fun ByteArray.clear() {
        for (i in indices) this[i] = 0
    }
}