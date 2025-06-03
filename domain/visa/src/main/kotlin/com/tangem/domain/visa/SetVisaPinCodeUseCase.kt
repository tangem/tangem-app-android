@file:Suppress("MaximumLineLength")

package com.tangem.domain.visa

import android.util.Base64
import arrow.core.Either
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.model.VisaEncryptedPinCode
import com.tangem.domain.visa.repository.VisaActivationRepository
import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

private const val KEY_SIZE = 256

class SetVisaPinCodeUseCase(private val visaActivationRepositoryFactory: VisaActivationRepository.Factory) {

    @Suppress("MagicNumber")
    suspend operator fun invoke(
        visaCardId: VisaCardId,
        activationOrderId: String,
        pinCode: String,
    ): Either<Throwable, Unit> = Either.catch {
        val visaActivationRepository = visaActivationRepositoryFactory.create(visaCardId)
        val rsaPublicKey = visaActivationRepository.getPinCodeRsaEncryptionPublicKey()
        val formattedPin = "24$pinCode${"f".repeat(n = 8)}FF"

        val sessionKey = generateSessionKey()
        val sessionId = getSessionId(rsaPublicKey, sessionKey)

        val iv = generateIV()
        val ivParameterSpec = IvParameterSpec(iv)
        val ivPayload = Base64.encodeToString(iv, Base64.NO_WRAP)
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey, ivParameterSpec)
        val encryptedPin = Base64.encodeToString(aesCipher.doFinal(formattedPin.encodeToByteArray()), Base64.NO_WRAP)

        visaActivationRepository.sendPinCode(
            VisaEncryptedPinCode(
                activationOrderId = activationOrderId,
                sessionId = sessionId,
                iv = ivPayload,
                encryptedPin = encryptedPin,
            ),
        )
    }

    private fun generateSessionKey(): Key {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(KEY_SIZE)
        return generator.generateKey()
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(size = 16)
        SecureRandom().nextBytes(iv)
        return iv
    }

    private fun getPublicKey(rsaPublicKey: String): PublicKey {
        val keyBytes = Base64.decode(rsaPublicKey, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private fun getSessionId(rsaPublicKey: String, sessionKey: Key): String {
        val publicKey = getPublicKey(rsaPublicKey)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val base64SessionKey = Base64.encodeToString(sessionKey.encoded, Base64.NO_WRAP)
        return Base64.encodeToString(cipher.doFinal(base64SessionKey.encodeToByteArray()), Base64.NO_WRAP)
    }
}