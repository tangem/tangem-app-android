package com.tangem.domain.tokens.operations

import arrow.core.NonEmptySet
import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.core.raise.DelegatedRaise
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class TokenListSortingOperations<E>(
    private val tokens: Set<TokenStatus>,
    private val isAnyTokenLoading: Boolean,
    private val sortByBalance: Boolean,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<E>,
    transformError: (Error) -> E,
) : DelegatedRaise<TokenListSortingOperations.Error, E>(raise, transformError) {

    constructor(
        tokenList: TokenList,
        dispatchers: CoroutineDispatcherProvider,
        raise: Raise<E>,
        transformError: (Error) -> E,
        sortByBalance: Boolean = tokenList.sortedBy == TokenList.SortType.BALANCE,
        isAnyTokenLoading: Boolean = tokenList.totalFiatBalance is TokenList.FiatBalance.Loading,
    ) : this(
        tokens = when (tokenList) {
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap { it.tokens }.toSet()
            is TokenList.Ungrouped -> tokenList.tokens
            is TokenList.NotInitialized -> emptySet()
        },
        isAnyTokenLoading = isAnyTokenLoading,
        sortByBalance = sortByBalance,
        dispatchers = dispatchers,
        raise = raise,
        transformError = transformError,
    )

    suspend fun getGroupedTokens(networks: Set<Network>): NonEmptySet<NetworkGroup> {
        return withContext(dispatchers.default) {
            ensure(tokens.isNotEmpty()) { Error.EmptyTokens }
            val networksNes = ensureNotNull(networks.toNonEmptySetOrNull()) {
                Error.EmptyNetworks
            }

            if (sortByBalance) {
                groupAndSortTokensByBalance(networksNes)
            } else {
                groupTokens(networksNes)
            }
        }
    }

    suspend fun getTokens(): NonEmptySet<TokenStatus> {
        return withContext(dispatchers.default) {
            val tokensNes = ensureNotNull(tokens.toNonEmptySetOrNull()) {
                Error.EmptyTokens
            }

            if (sortByBalance) sortTokensByBalance(tokensNes) else tokensNes
        }
    }

    fun getSortType() = if (sortByBalance) TokenList.SortType.BALANCE else TokenList.SortType.NONE

    private fun groupTokens(networks: NonEmptySet<Network>): NonEmptySet<NetworkGroup> {
        val groupedTokens = tokens
            .groupBy { it.networkId }
            .map { (networkId, tokens) ->
                val network = ensureNotNull(networks.firstOrNull { it.id == networkId }) {
                    Error.NetworkNotFound(networkId)
                }

                NetworkGroup(
                    network = network,
                    tokens = ensureNotNull(tokens.toNonEmptySetOrNull()) { Error.EmptyTokens },
                )
            }
            .toNonEmptySetOrNull()

        return ensureNotNull(groupedTokens) { Error.EmptyTokens }
    }

    private fun groupAndSortTokensByBalance(networks: NonEmptySet<Network>): NonEmptySet<NetworkGroup> {
        val groupsWithSortedTokens = groupTokens(networks)
            .map { group ->
                val tokens = group.tokens as? NonEmptySet<TokenStatus>
                    ?: error("Tokens can not be empty here")
                group.copy(tokens = sortTokensByBalance(tokens))
            }
            .toNonEmptySet()

        return if (isAnyTokenLoading) {
            groupsWithSortedTokens
        } else {
            sortGroupsByBalance(groupsWithSortedTokens)
        }
    }

    private fun sortTokensByBalance(tokens: NonEmptySet<TokenStatus>): NonEmptySet<TokenStatus> {
        return if (isAnyTokenLoading) {
            tokens
        } else {
            tokens.sortedByDescending { it.value.fiatAmount ?: BigDecimal.ZERO }
                .toNonEmptySetOrNull()
                ?: error("Tokens can not be empty here")
        }
    }

    private fun sortGroupsByBalance(groupsWithSortedTokens: NonEmptySet<NetworkGroup>): NonEmptySet<NetworkGroup> {
        return groupsWithSortedTokens
            .sortedByDescending { group ->
                group.tokens.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO }
            }
            .toNonEmptySetOrNull()
            ?: error("Tokens can not be empty here")
    }

    sealed class Error {

        object EmptyTokens : Error()

        object EmptyNetworks : Error()

        data class NetworkNotFound(val networkId: Network.ID) : Error()
    }
}