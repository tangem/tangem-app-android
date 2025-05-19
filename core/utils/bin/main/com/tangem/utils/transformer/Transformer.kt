package com.tangem.utils.transformer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Transforms state to updated state.
 */
interface Transformer<S> {
    fun transform(prevState: S): S
}

fun <T> MutableStateFlow<T>.update(transformer: Transformer<T>) {
    update { transformer.transform(this.value) }
}