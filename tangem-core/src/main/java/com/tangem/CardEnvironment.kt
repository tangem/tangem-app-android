package com.tangem


data class CardEnvironment(
        val pin1: String = DEFAULT_PIN,
        val pin2: String = DEFAULT_PIN2,
        val cardId: String? = null,
        val terminalKeys: KeyPair? = null,
        val encryptionKey: ByteArray? = null
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

class KeyPair(val publicKey: ByteArray, val privateKey: ByteArray)
