package com.tangem.tap.common.extensions

/**
[REDACTED_AUTHOR]
 */
fun <T> List<T>.containsAny(list: List<T>): Boolean {
    this.forEach { mainItem ->
        list.forEach { if (it == mainItem) return true }
    }
    return false
}