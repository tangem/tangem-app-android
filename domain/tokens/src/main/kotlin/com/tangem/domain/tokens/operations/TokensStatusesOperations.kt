package com.tangem.domain.tokens.operations

import arrow.core.NonEmptySet
import arrow.core.raise.Raise
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.core.raise.DelegatedRaise
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class TokensStatusesOperations<E>(
    private val tokensRepository: TokensRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val refresh: Boolean,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<E>,
    transformError: (Error) -> E,
) : DelegatedRaise<TokensStatusesOperations.Error, E>(raise, transformError) {

    constructor(
        userWalletId: UserWalletId,
        refresh: Boolean,
        useCase: GetTokenListUseCase,
        raise: Raise<E>,
        transformError: (Error) -> E,
    ) : this(
        tokensRepository = useCase.tokensRepository,
        quotesRepository = useCase.quotesRepository,
        networksRepository = useCase.networksRepository,
        userWalletId = userWalletId,
        refresh = refresh,
        dispatchers = useCase.dispatchers,
        raise = raise,
        transformError = transformError,
    )

    fun getTokensStatusesFlow(): Flow<Set<TokenStatus>> {
        return getTokens().flatMapConcat {
            val tokens = it.toNonEmptySetOrNull()

            if (tokens == null) {
                flowOf(emptySet())
            } else {
                val tokensIds = tokens.map { token -> token.id }.toNonEmptySet()
                val groupedTokens = groupTokens(tokens)

                combine(getQuotes(tokensIds), getNetworksStatues(groupedTokens)) { quotes, networksStatuses ->
                    createTokensStatuses(tokens, quotes, networksStatuses)
                }
            }
        }
    }

    private suspend fun createTokensStatuses(
        tokens: Set<Token>,
        quotes: Set<Quote>,
        networkStatuses: Set<NetworkStatus>,
    ): Set<TokenStatus> = withContext(dispatchers.single) {
        tokens.mapTo(hashSetOf()) { token ->
            val quote = quotes.firstOrNull { it.tokenId == token.id }
            val networkStatus = networkStatuses.firstOrNull { it.networkId == token.networkId }

            createStatus(token, quote, networkStatus)
        }
    }

    private suspend fun createStatus(token: Token, quote: Quote?, networkStatus: NetworkStatus?): TokenStatus {
        val tokenStatusOperations = TokenStatusOperations(
            token = token,
            quote = quote,
            networkStatus = networkStatus,
            dispatchers = dispatchers,
        )

        return tokenStatusOperations.createTokenStatus()
    }

    private fun getTokens(): Flow<Set<Token>> {
        return tokensRepository.getTokens(userWalletId, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyTokens) }
            .flowOn(dispatchers.io)
    }

    private fun getQuotes(tokensIds: NonEmptySet<Token.ID>): Flow<Set<Quote>> {
        return quotesRepository.getQuotes(tokensIds, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyQuotes) }
            .flowOn(dispatchers.io)
    }

    private fun getNetworksStatues(groupedTokens: Map<Network.ID, NonEmptySet<Token.ID>>): Flow<Set<NetworkStatus>> {
        return networksRepository.getNetworkStatuses(userWalletId, groupedTokens, refresh)
            .catch { raise(Error.DataError(it)) }
            .onEmpty { raise(Error.EmptyNetworksStatuses) }
            .flowOn(dispatchers.io)
    }

    private suspend fun groupTokens(tokens: NonEmptySet<Token>): Map<Network.ID, NonEmptySet<Token.ID>> =
        withContext(dispatchers.single) {
            tokens
                .groupBy { it.networkId }
                .mapValues { (_, tokens) ->
                    // Can not be empty
                    tokens.toNonEmptySetOrNull()!!
                        .map { it.id }
                        .toNonEmptySet()
                }
        }

    sealed class Error {

        object EmptyTokens : Error()

        object EmptyQuotes : Error()

        object EmptyNetworksStatuses : Error()

        data class DataError(val cause: Throwable) : Error()
    }
}
