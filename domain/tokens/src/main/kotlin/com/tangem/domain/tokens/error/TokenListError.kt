package com.tangem.domain.tokens.error

import com.tangem.domain.models.tokenlist.TokenList

sealed class TokenListError {

    data object EmptyTokens : TokenListError()

    data class UnableToSortTokenList(val unsortedTokenList: TokenList.Ungrouped) : TokenListError()

    data class DataError(val cause: Throwable) : TokenListError()
}