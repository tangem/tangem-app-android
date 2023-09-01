package com.tangem.domain.tokens.models.remove

sealed class RemoveCurrencyError : Throwable() {
    data class DataError(override val cause: Throwable) : RemoveCurrencyError()
}