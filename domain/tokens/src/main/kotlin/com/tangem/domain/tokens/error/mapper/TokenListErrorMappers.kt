package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.operations.TokenListOperations
import com.tangem.domain.tokens.operations.TokensStatusesOperations

internal fun TokensStatusesOperations.Error.mapToTokenListError(): TokenListError {
    return when (this) {
        is TokensStatusesOperations.Error.DataError -> TokenListError.DataError(this.cause)
        is TokensStatusesOperations.Error.EmptyNetworksStatuses,
        is TokensStatusesOperations.Error.EmptyQuotes,
        is TokensStatusesOperations.Error.EmptyTokens,
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