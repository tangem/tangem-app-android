package com.tangem.domain.common

/**
[REDACTED_AUTHOR]
 */
interface DomainError {
    val code: Int
    val message: String
    val data: Any?
}

open class AnyError(
    override val code: Int,
    override val message: String,
    override val data: Any? = null,
) : DomainError

interface ErrorConverter<T> {
    fun convertError(error: DomainError): T
}

interface Validator<Data, Error> {
    fun validate(data: Data? = null): Error?
}