package com.tangem.crypto

import com.tangem.commands.EllipticCurve
import java.security.SecureRandom

fun generateRandomBytes(length: Int): ByteArray {
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return bytes
}

fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray,
           curve: EllipticCurve = EllipticCurve.Secp256k1): Boolean {
    return when (curve) {
        EllipticCurve.Secp256k1 -> verifySecp256k1(publicKey, message, signature)
        EllipticCurve.Ed25519 -> verifyEd25519(publicKey, message, signature)
    }
}


