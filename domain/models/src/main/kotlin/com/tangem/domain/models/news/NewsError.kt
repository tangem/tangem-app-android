package com.tangem.domain.models.news

sealed interface NewsError {

    data class HttpError(
        val code: Int,
        val message: String,
    ) : NewsError

    data object NotHttpError : NewsError
}