package com.tangem.crypto

import com.tangem.commands.EllipticCurve
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object CryptoUtils {

    fun initCrypto() {
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        Security.addProvider(EdDSASecurityProvider())
    }

    /**
     * Generates ByteArray of random bytes.
     * It is used, among other things, to generate helper private keys
     * (not the one for the blockchains, that one is generated on the card and does not leave the card).
     *
     * @param length length of the ByteArray that is to be generated.
     */
    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /**
     * Helper function to verify that the data was signed with a private key that corresponds
     * to the provided public key.
     *
     * @param publicKey Corresponding to the private key that was used to sing a message
     * @param message The data that was signed
     * @param signature Signed data
     * @param curve Elliptic curve used
     *
     * @return Result of a verification
     */
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray,
               curve: EllipticCurve = EllipticCurve.Secp256k1): Boolean {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.verify(publicKey, message, signature)
            EllipticCurve.Ed25519 -> Ed25519.verify(publicKey, message, signature)
        }
    }

    /**
     * Helper function that generates public key from a private key.
     *
     * @param privateKeyArray  A private key from which a public key is generated
     * @param curve Elliptic curve used
     *
     * @return Public key [ByteArray]
     */
    fun generatePublicKey(
            privateKeyArray: ByteArray,
            curve: EllipticCurve = EllipticCurve.Secp256k1
    ): ByteArray {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.generatePublicKey(privateKeyArray)
            EllipticCurve.Ed25519 -> Ed25519.generatePublicKey(privateKeyArray)
        }
    }

    fun loadPublicKey(
            publicKey: ByteArray,
            curve: EllipticCurve = EllipticCurve.Secp256k1
    ): PublicKey {
        return when (curve) {
            EllipticCurve.Secp256k1 -> Secp256k1.loadPublicKey(publicKey)
            EllipticCurve.Ed25519 -> Ed25519.loadPublicKey(publicKey)
        }
    }
}

/**
 * Extension function to sign a ByteArray with an elliptic curve cryptography.
 *
 * @param privateKeyArray Key to sign data
 * @param curve Elliptic curve that is used to sign data
 *
 * @return Signed data
 */
fun ByteArray.sign(privateKeyArray: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): ByteArray {
    return when (curve) {
        EllipticCurve.Secp256k1 -> Secp256k1.sign(this, privateKeyArray)
        EllipticCurve.Ed25519 -> Ed25519.sign(this, privateKeyArray)
    }
}

fun ByteArray.encrypt(key: ByteArray, usePkcs7: Boolean = true): ByteArray {
    val spec = if (usePkcs7) ENCRYPTION_SPEC_PKCS7 else ENCRYPTION_SPEC_NO_PADDING
    val secretKeySpec = SecretKeySpec(key, spec)
    val cipher = Cipher.getInstance(spec, "SC")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(ByteArray(16)))
    return cipher.doFinal(this)
}

fun ByteArray.decrypt(key: ByteArray, usePkcs7: Boolean = true): ByteArray {
    val spec = if (usePkcs7) ENCRYPTION_SPEC_PKCS7 else ENCRYPTION_SPEC_NO_PADDING
    val secretKeySpec = SecretKeySpec(key, spec)
    val cipher = Cipher.getInstance(spec)
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(ByteArray(16)))
    return cipher.doFinal(this.copyOfRange(0, this.size))
}

fun ByteArray.pbkdf2Hash(salt: ByteArray, iterations: Int): ByteArray {
    return Pbkdf2().deriveKey(this, salt, iterations)
}

private const val ENCRYPTION_SPEC_PKCS7 = "AES/CBC/PKCS7PADDING"
private const val ENCRYPTION_SPEC_NO_PADDING = "AES/CBC/NOPADDING"




