package com.tangem.crypto

import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1Integer
import org.spongycastle.asn1.DERSequence
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature

internal fun verifySecp256k1(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
    val signatureInstance = Signature.getInstance("SHA256withECDSA")
    val loadedPublicKey = loadPublicKey(publicKey)
    signatureInstance.initVerify(loadedPublicKey)
    signatureInstance.update(message)

    val v = ASN1EncodableVector()
    val size = signature.size / 2
    v.add(calculateR(signature, size))
    v.add(calculateS(signature, size))
    val sigDer = DERSequence(v).encoded

    return signatureInstance.verify(sigDer)
}

private fun loadPublicKey(publicKeyArray: ByteArray): PublicKey {

    val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val factory = KeyFactory.getInstance("EC", "SC")

    val p1 = spec.curve.decodePoint(publicKeyArray)
    val keySpec = ECPublicKeySpec(p1, spec)

    return factory.generatePublic(keySpec)
}

private fun calculateR(signature: ByteArray, size: Int): ASN1Integer =
        ASN1Integer(BigInteger(1, signature.copyOfRange(0, size)))

private fun calculateS(signature: ByteArray, size: Int): ASN1Integer =
        ASN1Integer(BigInteger(1, signature.copyOfRange(size, size * 2)))