package com.tangem



data class CardEnvironment(
        val pin1: String = DEFAULT_PIN,
        val pin2: String = DEFAULT_PIN2,
        val terminalPublicKey: ByteArray? = null,
        val terminalPrivateKey: ByteArray? = null
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