package com.tangem.common

/**
[REDACTED_AUTHOR]
 */
interface Validator<Data, Error> {
    fun validate(data: Data? = null): Error?
}