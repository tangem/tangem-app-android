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
import javax.crypto.spec.SecretKeySpec

private const val KEY_SIZE = 256

class SetVisaPinCodeUseCase(
    private val visaActivationRepositoryFactory: VisaActivationRepository.Factory,
) {

    suspend operator fun invoke(
        visaCardId: VisaCardId,
        activationOrderId: String,
        pinCode: String,
    ): Either<Throwable, Unit> = Either.catch {
        val visaActivationRepository = visaActivationRepositoryFactory.create(visaCardId)
        val rsaPublicKey = visaActivationRepository.getPinCodeRsaEncryptionPublicKey()

        val sessionKey = generateSessionKey()
        val sessionId = getSessionId(rsaPublicKey, sessionKey)

        val secureRandom = SecureRandom()
        val iv = ByteArray(size = 16)
        secureRandom.nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)
        val ivPayload = Base64.encodeToString(iv, Base64.NO_WRAP)
        val aesKey = SecretKeySpec(sessionKey.encoded, 0, sessionKey.encoded.size, "AES")
        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivParameterSpec)
        val encryptedPin = Base64.encodeToString(aesCipher.doFinal(pinCode.toByteArray()), Base64.NO_WRAP)

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

    private fun getPublicKey(rsaPublicKey: String): PublicKey {
        val keyBytes = Base64.decode(rsaPublicKey, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private fun getSessionId(rsaPublicKey: String, sessionKey: Key): String {
        val publicKey = getPublicKey(rsaPublicKey)
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeToString(cipher.doFinal(sessionKey.encoded), Base64.NO_WRAP)
    }
}