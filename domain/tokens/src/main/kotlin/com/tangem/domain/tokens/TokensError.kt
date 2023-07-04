package com.tangem.domain.tokens

sealed interface TokensError

internal sealed interface InternalTokensError : TokensError

internal object TokensNotFetched : InternalTokensError

internal object GroupingTypeNotFetched : InternalTokensError

internal object SortingTypeNotFetched : InternalTokensError

object EmptyTokens : TokensError
