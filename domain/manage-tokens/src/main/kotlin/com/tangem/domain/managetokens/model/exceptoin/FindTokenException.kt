package com.tangem.domain.managetokens.model.exceptoin

sealed class FindTokenException {

    data object NotFound : FindTokenException()

    data class DataError(val cause: Throwable) : FindTokenException()
}