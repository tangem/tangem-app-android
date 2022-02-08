package com.tangem.tap.common.extensions

/**
[REDACTED_AUTHOR]
 */
fun StringBuilder.appendIf(value: String, predicate: (String) -> Boolean) {
    if (predicate(value)) this.append(value)
}