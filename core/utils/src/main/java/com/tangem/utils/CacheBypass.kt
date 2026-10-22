package com.tangem.utils

/**
 * Appends a unique query parameter so that browsers and PDF viewers load the current version of a document
 * instead of a cached one: documents are replaced under the same URL.
 */
fun String.withCacheBypass(): String = withCacheBypass(timestamp = System.currentTimeMillis())

internal fun String.withCacheBypass(timestamp: Long): String {
    val fragmentStart = indexOf('#').takeIf { it >= 0 } ?: length
    val base = substring(0, fragmentStart)
    val separator = if ('?' in base) '&' else '?'
    return "$base${separator}t=$timestamp${substring(fragmentStart)}"
}