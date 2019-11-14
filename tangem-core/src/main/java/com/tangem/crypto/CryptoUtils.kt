package com.tangem.crypto

import com.tangem.commands.EllipticCurve
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import java.security.SecureRandom
import java.security.Security


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
            EllipticCurve.Secp256k1 -> Sepc256k1.verify(publicKey, message, signature)
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
            EllipticCurve.Secp256k1 -> Sepc256k1.generatePublicKey(privateKeyArray)
            EllipticCurve.Ed25519 -> Ed25519.generatePublicKey(privateKeyArray)
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
        EllipticCurve.Secp256k1 -> signSecp256k1(this, privateKeyArray)
        EllipticCurve.Ed25519 -> signEd25519(this, privateKeyArray)
    }
}


