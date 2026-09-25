package com.tangem.data.walletconnect.pair

/**
 * Minimal reader for the WalletConnect v2 pairing URI: `wc:<pairingTopic>@<version>?<params>`.
 */
internal object WcPairingUri {

    private const val SCHEME = "wc:"
    private const val VERSION_DELIMITER = '@'
    private const val TOPIC_LENGTH = 64

    /**
     * The pairing topic of [uri], or `null` when the string is not a recognisable `wc:` URI (the SDK will
     * reject it on its own; the caller then simply does not filter proposals by topic).
     */
    fun topicOf(uri: String): String? {
        val trimmed = uri.trim()
        // deep-link wrappers such as tangem://wc?uri=wc%3A… are unwrapped by the caller; accept a bare `wc:`
        val body = trimmed.substringAfter(SCHEME, missingDelimiterValue = "")
        if (body.isEmpty()) return null
        val topic = body.substringBefore(VERSION_DELIMITER)
        val isHexTopic = topic.length == TOPIC_LENGTH && topic.all(::isHexDigit)
        return if (isHexTopic) topic.lowercase() else null
    }

    private fun isHexDigit(char: Char): Boolean = char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'
}
