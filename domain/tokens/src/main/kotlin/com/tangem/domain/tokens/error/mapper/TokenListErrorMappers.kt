package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations

internal fun CurrenciesStatusesOperations.Error.mapToTokenListError(): TokenListError {
    return when (this) {
        is CurrenciesStatusesOperations.Error.DataError -> TokenListError.DataError(this.cause)
        is CurrenciesStatusesOperations.Error.EmptyNetworksStatuses,
        is CurrenciesStatusesOperations.Error.EmptyQuotes,
        is CurrenciesStatusesOperations.Error.EmptyCurrencies,
        is CurrenciesStatusesOperations.Error.EmptyAddresses,
        is CurrenciesStatusesOperations.Error.UnableToCreateCurrencyStatus,
        is CurrenciesStatusesOperations.Error.EmptyStakingBalances,
        -> TokenListError.EmptyTokens
    }
}