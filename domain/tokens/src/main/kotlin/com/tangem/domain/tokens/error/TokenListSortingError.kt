package com.tangem.domain.tokens.error

import com.tangem.domain.tokens.operations.TokenListSortingOperations

sealed class TokenListSortingError {

    object TokenListIsLoading : TokenListSortingError()

    object TokenListIsEmpty : TokenListSortingError()

    object UnableToSortTokenList : TokenListSortingError()

    data class DataError(val cause: Throwable) : TokenListSortingError()

    internal companion object {

        fun fromTokeListSortingOperations(error: TokenListSortingOperations.Error): TokenListSortingError {
            return when (error) {
                is TokenListSortingOperations.Error.EmptyTokens -> TokenListIsEmpty
                is TokenListSortingOperations.Error.EmptyNetworks,
                is TokenListSortingOperations.Error.NetworkNotFound,
                -> UnableToSortTokenList
            }
        }
    }
}