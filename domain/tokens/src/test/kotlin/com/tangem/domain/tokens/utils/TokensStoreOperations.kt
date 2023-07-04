package com.tangem.domain.tokens.utils

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.raise.either
import com.tangem.domain.tokens.TokensError
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.store.TokensStore
import kotlinx.coroutines.flow.first

internal suspend fun TokensStore.getTokens(): Either<TokensError, NonEmptySet<Token>> {
    return either { getTokens().first() }
}

internal suspend fun TokensStore.getQuotes(): Either<TokensError, Set<Quote>> {
    return either { getQuotes().first() }
}

internal suspend fun TokensStore.getNetworkStatuses(): Either<TokensError, Set<NetworkStatus>> {
    return either { getNetworksStatuses().first() }
}

internal suspend fun TokensStore.getIsGrouped(): Either<TokensError, Boolean> {
    return either { getIsTokensGrouped().first() }
}

internal suspend fun TokensStore.getIsSortedByBalance(): Either<TokensError, Boolean> {
    return either { getIsTokensSortedByBalance().first() }
}
