package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.operations.TokenListOperations

internal fun CurrenciesStatusesOperations.Error.mapToTokenListError(): TokenListError {
    return when (this) {
        is CurrenciesStatusesOperations.Error.DataError -> TokenListError.DataError(this.cause)
        is CurrenciesStatusesOperations.Error.EmptyNetworksStatuses,
        is CurrenciesStatusesOperations.Error.EmptyQuotes,
        is CurrenciesStatusesOperations.Error.EmptyCurrencies,
        is CurrenciesStatusesOperations.Error.UnableToCreateCurrencyStatus,
        -> TokenListError.EmptyTokens
    }
}

internal fun TokenListOperations.Error.mapToTokenListError(): TokenListError {
    return when (this) {
        is TokenListOperations.Error.DataError -> TokenListError.DataError(this.cause)
        is TokenListOperations.Error.UnableToSortTokenList ->
            TokenListError.UnableToSortTokenList(this.unsortedTokenList)
        is TokenListOperations.Error.UnableToGroupTokenList ->
            TokenListError.UnableToSortTokenList(this.ungroupedTokenList)
    }
}