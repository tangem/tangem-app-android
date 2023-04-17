package com.tangem.utils.extensions

/**
 * @author Anton Zhilenkov on 16.04.2023.
 */
fun Int.isEven(): Boolean = this % 2 == 0

fun Int.isOdd(): Boolean = !this.isEven()
