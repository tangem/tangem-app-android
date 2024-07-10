package com.tangem.utils.transformer

/**
 * Transforms state to updated state.
 */
interface Transformer<S> {
    fun transform(prevState: S): S
}