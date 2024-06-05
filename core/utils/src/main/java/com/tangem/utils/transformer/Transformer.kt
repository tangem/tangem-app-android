package com.tangem.utils.transformer

interface Transformer<S, V> {
    fun transform(prevState: S, value: V): S
}