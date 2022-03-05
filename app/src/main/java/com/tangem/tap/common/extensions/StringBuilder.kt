package com.tangem.tap.common.extensions

/**
[REDACTED_AUTHOR]
 */
fun StringBuilder.appendIf(value: String, predicate: (String) -> Boolean): StringBuilder {
    if (predicate(value)) this.append(value)
    return this
}

fun StringBuilder.appendIfNotNull(value: String?, prefix: String? = null, postfix: String? = null): StringBuilder {
    if (value == null) return this

    prefix?.let { append(it) }
    append(value)
    postfix?.let { append(it) }
    return this
}

fun String.appendIfNotNull(value: String?, prefix: String? = null, postfix: String? = null): String {
    return StringBuilder(this).apply { appendIfNotNull(value, prefix, postfix) }.toString()
}