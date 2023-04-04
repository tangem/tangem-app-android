package com.tangem.tap.common.compose.extensions

import androidx.compose.runtime.MutableState

/**
 * Created by Anton Zhilenkov on 04/07/2022.
 */
fun <T> MutableState<List<T>>.addAndNotify(value: T) {
    this.value = this.value.toMutableList().apply { add(value) }
}

fun <T> MutableState<List<T>>.removeAndNotify(value: T) {
    this.value = this.value.toMutableList().apply { remove(value) }
}
