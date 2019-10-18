package com.tangem.crypto

import com.tangem.common.extentions.calculateSha512
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest
import java.security.PublicKey

internal fun verifyEd25519(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
    val messageSha512 = message.calculateSha512()
    val loadedPublicKey = loadPublicKey(publicKey)
    val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    val signatureInstance = EdDSAEngine(MessageDigest.getInstance(spec.hashAlgorithm))
    signatureInstance.initVerify(loadedPublicKey)

    signatureInstance.update(messageSha512)

    return signatureInstance.verify(signature)
}

private fun loadPublicKey(publicKeyArray: ByteArray): PublicKey {
    val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    val pubKey = EdDSAPublicKeySpec(publicKeyArray, spec)
    return EdDSAPublicKey(pubKey)
}