package com.tangem.datasource.config

/**
* [REDACTED_AUTHOR]
 */
interface Loader<T> {
    fun load(onComplete: (T) -> Unit)
}
