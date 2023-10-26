package com.tangem.domain.tokens.model.remove

sealed class RemoveCurrencyError : Throwable() {
    data class DataError(override val cause: Throwable) : RemoveCurrencyError()

    object HasLinkedTokens : RemoveCurrencyError()
}