package com.tangem.domain.appcurrency.error

sealed class SelectedAppCurrencyError {

    object NoAppCurrencySelected : SelectedAppCurrencyError()

    data class DataError(val cause: Throwable) : SelectedAppCurrencyError()
}
