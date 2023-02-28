package com.tangem.datasource.api.paymentology.models.response

import com.tangem.common.services.Result

interface ResponseError {
    val success: Boolean
    val error: String?
    val errorCode: Int?

    fun makeErrorMessage(): String = error ?: "unknown error"
}

inline fun <reified T> ResponseError.tryExtractError(): Result<T> = when (success) {
    true -> Result.Success(this as T)
    else -> Result.Failure(Throwable(makeErrorMessage()))
}
