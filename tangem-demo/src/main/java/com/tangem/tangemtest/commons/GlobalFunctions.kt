package com.tangem.tangemtest.commons

/**
[REDACTED_AUTHOR]
 */
fun <A, B> performAction(a: A?, b: B?, action: (A, B) -> Unit) {
    if (a != null && b != null) action(a, b)
}