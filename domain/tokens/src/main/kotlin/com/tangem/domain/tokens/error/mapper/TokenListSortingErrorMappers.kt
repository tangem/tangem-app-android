package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.operations.TokenListSortingOperations

internal fun TokenListSortingOperations.Error.mapToTokenListSortingError(): TokenListSortingError {
    return when (this) {
        is TokenListSortingOperations.Error.EmptyTokens -> TokenListSortingError.TokenListIsEmpty
        is TokenListSortingOperations.Error.EmptyNetworks,
        is TokenListSortingOperations.Error.NetworkNotFound,
        -> TokenListSortingError.UnableToSortTokenList
    }
}