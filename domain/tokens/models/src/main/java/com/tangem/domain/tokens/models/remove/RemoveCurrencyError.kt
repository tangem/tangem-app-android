package com.tangem.domain.tokens.models.remove

import com.tangem.domain.tokens.models.CryptoCurrency

sealed class RemoveCurrencyError : Throwable() {
    data class HasLinkedTokens(val currency: CryptoCurrency) : RemoveCurrencyError()
    data class DataError(override val cause: Throwable) : RemoveCurrencyError()
}