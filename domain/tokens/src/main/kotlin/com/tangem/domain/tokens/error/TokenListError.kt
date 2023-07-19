package com.tangem.domain.tokens.error

import com.tangem.domain.tokens.model.TokenList

sealed class TokenListError {

    object EmptyTokens : TokenListError()

    data class UnableToSortTokenList(val unsortedTokenList: TokenList.Ungrouped) : TokenListError()

    data class DataError(val cause: Throwable) : TokenListError()
}