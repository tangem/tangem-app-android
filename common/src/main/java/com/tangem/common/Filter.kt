package com.tangem.common

/**
[REDACTED_AUTHOR]
 */
interface Filter<T> {
    fun filter(value: T): Boolean
}