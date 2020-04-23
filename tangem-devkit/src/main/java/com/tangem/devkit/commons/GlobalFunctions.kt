package com.tangem.devkit.commons

/**
[REDACTED_AUTHOR]
 */
fun <A, B> performAction(a: A?, b: B?, action: (A, B) -> Unit, onFail: (() -> Unit)? = null) {
    if (a != null && b != null) action(a, b)
    else onFail?.invoke()
}