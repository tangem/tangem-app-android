package com.tangem.domain.tokens.utils

import arrow.core.NonEmptySet
import arrow.core.raise.Raise
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class TokensStatusesOperations(
    private val tokensRepository: TokensRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val refresh: Boolean,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<TokensError>,
) : Raise<TokensError> by raise {

    fun getTokensStatusesFlow(): Flow<Set<TokenStatus>> {
        return getTokens().flatMapConcat {
            val tokens = it.toNonEmptySetOrNull()

            if (tokens == null) {
                flowOf(emptySet())
            } else {
                val tokensIds = tokens.map { token -> token.id }
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
        tokens.map { token ->
            val quote = quotes.firstOrNull { it.tokenId == token.id }
            val networkStatus = networkStatuses.firstOrNull { it.networkId == token.networkId }

            createStatus(token, quote, networkStatus)
        }.toSet()
    }

    private suspend fun createStatus(token: Token, quote: Quote?, networkStatus: NetworkStatus?): TokenStatus {
        val operations = TokenStatusOperations(token, quote, networkStatus, dispatchers, raise = this)
        return operations.createTokenStatus()
    }

    private fun getTokens(): Flow<Set<Token>> {
        return tokensRepository.getTokens(userWalletId, refresh)
            .onEmpty { raise(TokensError.EmptyTokens) }
            .map { it.bind() }
            .flowOn(dispatchers.io)
    }

    private fun getQuotes(tokensIds: NonEmptySet<Token.ID>): Flow<Set<Quote>> {
        return quotesRepository.getQuotes(tokensIds, refresh)
            .onEmpty { raise(TokensError.EmptyQuotes) }
            .map { it.bind() }
            .flowOn(dispatchers.io)
    }

    private fun getNetworksStatues(groupedTokens: Map<Network.ID, NonEmptySet<Token.ID>>): Flow<Set<NetworkStatus>> {
        return networksRepository.getNetworkStatuses(userWalletId, groupedTokens, refresh)
            .onEmpty { raise(TokensError.EmptyNetworkStatues) }
            .map { it.bind() }
            .flowOn(dispatchers.io)
    }

    private suspend fun groupTokens(tokens: NonEmptySet<Token>): Map<Network.ID, NonEmptySet<Token.ID>> =
        withContext(dispatchers.single) {
            tokens
                .groupBy { it.networkId }
                .mapValues { (_, tokens) ->
                    tokens.toNonEmptySetOrNull()
                        ?.map { it.id }
                        ?: raise(TokensError.EmptyTokens)
                }
        }
}