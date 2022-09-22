package com.tangem.common

/**
[REDACTED_AUTHOR]
 */
interface Converter<I, O> {
    fun convert(value: I): O
}