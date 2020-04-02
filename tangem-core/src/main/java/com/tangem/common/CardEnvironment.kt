package com.tangem.common

import com.tangem.commands.EllipticCurve
import com.tangem.crypto.CryptoUtils.generatePublicKey


/**
 * Contains data relating to a Tangem card. It is used in constructing all the commands,
 * and commands can return modified [CardEnvironment].
 */
data class CardEnvironment(
        val pin1: String = DEFAULT_PIN,
        val pin2: String = DEFAULT_PIN2,
        val cardId: String? = null,
        val terminalKeys: KeyPair? = null,
        var encryptionMode: EncryptionMode = EncryptionMode.NONE,
        var encryptionKey: ByteArray? = null,
        val cvc: ByteArray? = null
) {

    companion object {
        const val DEFAULT_PIN = "000000"
        const val DEFAULT_PIN2 = "000"
    }
}

enum class EncryptionMode(val code: Byte) {
    NONE(0x0),
    FAST(0x1),
    STRONG(0x2)
}

class KeyPair(val publicKey: ByteArray, val privateKey: ByteArray) {

    constructor(privateKey: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1) :
            this(generatePublicKey(privateKey, curve), privateKey)
}