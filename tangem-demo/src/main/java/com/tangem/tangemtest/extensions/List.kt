package com.tangem.tangemtest.extensions

/**
[REDACTED_AUTHOR]
 */
fun <T> List<T>.print(delimiter: String = ", ", wrap: Boolean = true): String {
    val builder = StringBuilder()
    forEach { builder.append(it).append(delimiter) }
    val length = builder.length
    if (length > delimiter.length) {
        builder.delete(length - delimiter.length, length)
    }
    val result = builder.toString()

    return if (wrap) "[$result]" else result
}