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