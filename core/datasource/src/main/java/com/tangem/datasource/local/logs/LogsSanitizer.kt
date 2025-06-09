package com.tangem.datasource.local.logs

/**
 * Logs sanitizer
 *
[REDACTED_AUTHOR]
 */
internal object LogsSanitizer {

    private val HEX_REGEX = Regex("(0[xX])?(?:[A-Fa-f0-9]{2}-?){4,}")
    private const val HIDDEN_TEXT = "******"

    fun sanitize(value: String): String {
        return HEX_REGEX.replace(input = value, replacement = HIDDEN_TEXT)
    }
}