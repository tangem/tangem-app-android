package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations

internal fun CurrenciesStatusesOperations.Error.mapToCurrencyError(): CurrencyStatusError {
    return when (this) {
        is CurrenciesStatusesOperations.Error.DataError -> CurrencyStatusError.DataError(this.cause)
        is CurrenciesStatusesOperations.Error.EmptyYieldBalances,
        is CurrenciesStatusesOperations.Error.EmptyNetworksStatuses,
        is CurrenciesStatusesOperations.Error.EmptyQuotes,
        is CurrenciesStatusesOperations.Error.EmptyCurrencies,
        is CurrenciesStatusesOperations.Error.UnableToCreateCurrencyStatus,
        -> CurrencyStatusError.UnableToCreateCurrency
    }
}
