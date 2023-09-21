package com.tangem.domain.appcurrency.error

sealed class AvailableCurrenciesError {

    object CurrenciesIsEmpty : AvailableCurrenciesError()

    data class DataError(val cause: Throwable) : AvailableCurrenciesError()
}