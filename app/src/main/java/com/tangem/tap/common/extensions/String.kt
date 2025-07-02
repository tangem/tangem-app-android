package com.tangem.tap.common.extensions

fun String.removePrefixOrNull(prefix: String): String? = when {
    startsWith(prefix) -> substring(prefix.length)
    else -> null
}