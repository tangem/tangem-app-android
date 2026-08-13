package com.tangem.domain.jointaccount.signing

/**
 * JSON canonicalization per RFC 8785 (JCS) for the payloads this feature signs: the backend canonicalizes the
 * received payload the same way and verifies the EIP-191 signature over it, so both sides must produce identical
 * bytes.
 *
 * Covers the value types those payloads consist of — objects, arrays, strings, booleans, integral numbers and
 * `null`. Floating-point numbers are rejected on purpose: the ECMAScript number formatting RFC 8785 prescribes is
 * easy to get subtly wrong, and no signed payload contains one.
 */
object CanonicalJson {

    fun canonicalize(value: Map<String, Any?>): ByteArray {
        return buildString { appendValue(value) }.toByteArray(Charsets.UTF_8)
    }

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean, is Int, is Long -> append(value)
            is String -> appendString(value)
            is Map<*, *> -> appendObject(value)
            is List<*> -> appendArray(value)
            else -> error("Type is not supported in canonical JSON: ${value::class.qualifiedName}")
        }
    }

    private fun StringBuilder.appendObject(map: Map<*, *>) {
        val entries = map.entries
            .map { (key, value) ->
                requireNotNull(key as? String) { "Canonical JSON object keys must be strings, got: $key" } to value
            }
            // RFC 8785 orders properties by UTF-16 code units — exactly Kotlin's natural String order
            .sortedBy { (key, _) -> key }

        append('{')
        entries.forEachIndexed { index, (key, value) ->
            if (index > 0) append(',')
            appendString(key)
            append(':')
            appendValue(value)
        }
        append('}')
    }

    private fun StringBuilder.appendArray(list: List<*>) {
        append('[')
        list.forEachIndexed { index, value ->
            if (index > 0) append(',')
            appendValue(value)
        }
        append(']')
    }

    @Suppress("MagicNumber")
    private fun StringBuilder.appendString(value: String) {
        append('"')
        value.forEach { char ->
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\b' -> append("\\b")
                char == '\u000C' -> append("\\f")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char.code < 0x20 -> append("\\u%04x".format(char.code))
                else -> append(char)
            }
        }
        append('"')
    }
}