package com.tangem.domain.models.earn

sealed interface EarnError {

    data class HttpError(
        val code: Int,
        val message: String,
    ) : EarnError

    data object NotHttpError : EarnError
}