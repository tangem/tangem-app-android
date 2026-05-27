package com.tangem.tap.core.navigation.email

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Truncates an email body so the resulting Intent fits inside the per-process Binder buffer (1 MB).
 *
 * The chooser fans the Intent out to every installed email client (with extras duplicated per target),
 * so the body must be kept well below the raw 1 MB ceiling.
 */
internal class EmailMessageTruncator {

    fun truncate(message: String): String {
        val bytes = message.toByteArray(Charsets.UTF_8)
        if (bytes.size <= MAX_MESSAGE_BYTES) return message

        val suffix = TRUNCATION_SUFFIX_TEMPLATE.format(bytes.size)
        val suffixBytes = suffix.toByteArray(Charsets.UTF_8).size
        val cutSize = MAX_MESSAGE_BYTES - suffixBytes

        // Drop a partial UTF-8 sequence at the cut boundary rather than replacing it with U+FFFD
        // (which is 3 bytes in UTF-8 and would push the result over the cap).
        val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE)
        val head = decoder.decode(ByteBuffer.wrap(bytes, 0, cutSize)).toString()
        return head + suffix
    }

    private companion object {
        // Chooser duplicates EXTRA_TEXT once per target email app (EXTRA_INITIAL_INTENTS),
        // so parcel ≈ N × body. 20 KB clears the 1 MB Binder limit for up to ~30 mail clients.
        const val MAX_MESSAGE_BYTES = 20_000
        const val TRUNCATION_SUFFIX_TEMPLATE = "\n\n…[truncated, original %d bytes]"
    }
}