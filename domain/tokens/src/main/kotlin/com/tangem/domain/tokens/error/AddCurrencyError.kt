package com.tangem.domain.tokens.error

sealed class AddCurrencyError {

    data class DataError(val cause: Throwable) : AddCurrencyError()
}