package com.tangem.utils.extensions

/**
* [REDACTED_AUTHOR]
 */
fun Int.isEven(): Boolean = this % 2 == 0

fun Int.isOdd(): Boolean = !this.isEven()
