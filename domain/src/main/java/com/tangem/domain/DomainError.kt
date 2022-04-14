package com.tangem.domain

/**
[REDACTED_AUTHOR]
 * @property code describes what feature is the error coming from
 * @property message the error description
 * @property data any data that can help in the part where this error is being handled
 */
interface DomainError : DomainMessage {
    val code: Int
    val message: String
    val data: Any?
}

open class AnError(
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

const val ERROR_CODE_ADD_CUSTOM_TOKEN = 100