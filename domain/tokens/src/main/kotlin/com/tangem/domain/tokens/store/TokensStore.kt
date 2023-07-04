package com.tangem.domain.tokens.store

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.right
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.core.store.Store
import com.tangem.domain.tokens.GroupingTypeNotFetched
import com.tangem.domain.tokens.SortingTypeNotFetched
import com.tangem.domain.tokens.TokensError
import com.tangem.domain.tokens.TokensNotFetched
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class TokensStore internal constructor() : Store<TokensError, TokensStore.State>() {
    override val stateFlow: MutableSharedFlow<Either<TokensError, State>> = MutableSharedFlow(replay = 1)

    init {
        stateFlow.tryEmit(getInitialState().right())
    }

    override fun getInitialState(): State = State()

    internal suspend fun setTokens(tokens: NonEmptySet<Token>) = updateState {
        copy(tokens = tokens)
    }

    internal suspend fun setQuotes(quotes: NonEmptySet<Quote>) = updateState {
        copy(quotes = quotes)
    }

    internal suspend fun setIsTokensGrouped(isGrouped: Boolean) = updateState {
        copy(isTokensGrouped = isGrouped)
    }

    internal suspend fun setIsTokensSortedByBalance(isSorted: Boolean) = updateState {
        copy(isTokensSortedByBalance = isSorted)
    }

    internal suspend fun addOrUpdateNetworkStatus(networkStatus: NetworkStatus) = updateState {
        val indexOfNetworkStatus = networkStatuses.indexOfFirst { it.networkId == networkStatus.networkId }
        val networkStatusesMutable = networkStatuses.toMutableList()

        val updatedStatuses = networkStatusesMutable.apply {
            if (indexOfNetworkStatus == -1) {
                add(networkStatus)
            } else {
                set(indexOfNetworkStatus, networkStatus)
            }
        }

        copy(
            networkStatuses = updatedStatuses.toSet(),
        )
    }

    internal suspend fun setError(error: TokensError) = updateError { error }

    internal fun Raise<TokensError>.getTokens(): Flow<NonEmptySet<Token>> =
        getValues(State::tokens) { ensureNotNull(it.toNonEmptySetOrNull()) { TokensNotFetched } }

    internal fun Raise<TokensError>.getQuotes(): Flow<Set<Quote>> = getValues(State::quotes)

    internal fun Raise<TokensError>.getNetworksStatuses(): Flow<Set<NetworkStatus>> = getValues(State::networkStatuses)

    internal fun Raise<TokensError>.getIsTokensGrouped(): Flow<Boolean> =
        getValues(State::isTokensGrouped) { ensureNotNull(it) { GroupingTypeNotFetched } }

    internal fun Raise<TokensError>.getIsTokensSortedByBalance(): Flow<Boolean> =
        getValues(State::isTokensSortedByBalance) { ensureNotNull(it) { SortingTypeNotFetched } }

    data class State(
        val tokens: Set<Token> = emptySet(),
        val quotes: Set<Quote> = emptySet(),
        val networkStatuses: Set<NetworkStatus> = emptySet(),
        val isTokensGrouped: Boolean? = null,
        val isTokensSortedByBalance: Boolean? = null,
    )
}
