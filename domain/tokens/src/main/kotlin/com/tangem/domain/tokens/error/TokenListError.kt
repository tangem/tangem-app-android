package com.tangem.domain.tokens.error

import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.operations.TokenListOperations
import com.tangem.domain.tokens.operations.TokensStatusesOperations

sealed class TokenListError {

    object EmptyTokens : TokenListError()

    data class UnableToSortTokenList(val unsortedTokenList: TokenList.Ungrouped) : TokenListError()

    data class DataError(val cause: Throwable) : TokenListError()

    internal companion object {

        fun fromTokenStatusesOperations(error: TokensStatusesOperations.Error): TokenListError = when (error) {
            is TokensStatusesOperations.Error.DataError -> DataError(error.cause)
            is TokensStatusesOperations.Error.EmptyNetworksStatuses,
            is TokensStatusesOperations.Error.EmptyQuotes,
            is TokensStatusesOperations.Error.EmptyTokens,
            -> EmptyTokens
        }

        fun fromTokenListOperations(error: TokenListOperations.Error): TokenListError = when (error) {
            is TokenListOperations.Error.DataError -> DataError(error.cause)
            is TokenListOperations.Error.UnableToSortTokenList -> UnableToSortTokenList(error.unsortedTokenList)
            is TokenListOperations.Error.UnableToGroupTokenList -> UnableToSortTokenList(error.ungroupedTokenList)
        }
    }
}
