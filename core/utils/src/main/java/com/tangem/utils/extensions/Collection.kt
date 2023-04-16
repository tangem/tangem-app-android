package com.tangem.utils.extensions

/**
 * Created by Anton Zhilenkov on 06.04.2023.
 */
fun <T> Collection<T>.isSingleItem(): Boolean = this.size == 1
