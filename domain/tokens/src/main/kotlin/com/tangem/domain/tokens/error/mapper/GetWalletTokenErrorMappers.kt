package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations

internal fun CurrenciesStatusesOperations.Error.mapToCurrencyError(): CurrencyError {
    return when (this) {
        is CurrenciesStatusesOperations.Error.DataError -> CurrencyError.DataError(this.cause)
        is CurrenciesStatusesOperations.Error.EmptyNetworksStatuses,
        is CurrenciesStatusesOperations.Error.EmptyQuotes,
        is CurrenciesStatusesOperations.Error.EmptyCurrencies,
        is CurrenciesStatusesOperations.Error.UnableToCreateCurrencyStatus,
        -> CurrencyError.UnableToCreateCurrency
    }
}
