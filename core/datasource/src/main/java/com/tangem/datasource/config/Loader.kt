package com.tangem.datasource.config

/**
[REDACTED_AUTHOR]
 */
interface Loader<T> {
    suspend fun load(onComplete: (T) -> Unit)
}