package com.tangem.domain.tokens.error

sealed class TokenListSortingError {

    object TokenListIsLoading : TokenListSortingError()

    object TokenListIsEmpty : TokenListSortingError()

    object UnableToSortTokenList : TokenListSortingError()
}