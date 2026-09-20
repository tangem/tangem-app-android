package com.tangem.lib.auth.social

import android.util.Base64
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * Seals the recovery key for the KMS with JWA `RSA-OAEP-256`, the form the backend stores it in.
 *
 * The OAEP parameters are spelled out rather than taken from the `OAEPWithSHA-256AndMGF1Padding`
 * transform name: that name leaves MGF1 on SHA-1 in the JCE, which the service cannot unwrap.
 */
internal object RecoveryKeyRsaCipher {

    const val SUPPORTED_ALGORITHM = "RSA-OAEP-256"

    private const val TRANSFORMATION = "RSA/ECB/OAEPPadding"

    fun seal(modulusBase64Url: String, exponentBase64Url: String, plaintext: ByteArray): ByteArray {
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            RSAPublicKeySpec(
                modulusBase64Url.toUnsignedBigInteger(),
                exponentBase64Url.toUnsignedBigInteger(),
            ),
        )
        val parameters = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT,
        )

        return Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, publicKey, parameters)
            doFinal(plaintext)
        }
    }

    /** JWK numbers are unsigned big-endian base64url, so the leading bit must not be read as a sign */
    private fun String.toUnsignedBigInteger(): BigInteger =
        BigInteger(1, Base64.decode(this, Base64.URL_SAFE or Base64.NO_WRAP))
}