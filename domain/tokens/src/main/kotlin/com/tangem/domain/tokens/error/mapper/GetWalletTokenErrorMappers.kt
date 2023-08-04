package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.TokenError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations

internal fun CurrenciesStatusesOperations.Error.mapToTokenError(): TokenError {
    return when (this) {
        is CurrenciesStatusesOperations.Error.DataError -> TokenError.DataError(this.cause)
        is CurrenciesStatusesOperations.Error.EmptyNetworksStatuses,
        is CurrenciesStatusesOperations.Error.EmptyQuotes,
        is CurrenciesStatusesOperations.Error.EmptyCurrencies,
        is CurrenciesStatusesOperations.Error.UnableToCreateCurrencyStatus,
        -> TokenError.UnableToCreateToken
    }
}