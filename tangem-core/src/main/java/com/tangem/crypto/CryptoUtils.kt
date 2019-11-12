package com.tangem.crypto

import com.tangem.commands.EllipticCurve
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import java.security.SecureRandom
import java.security.Security

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


fun initCrypto() {
    Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
    Security.addProvider(EdDSASecurityProvider())
}


fun ByteArray.sign(privateKeyArray: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1): ByteArray {

    return when (curve) {
        EllipticCurve.Secp256k1 -> signSecp256k1(this,privateKeyArray)
        EllipticCurve.Ed25519 -> signEd25519(this, privateKeyArray)
    }


}



