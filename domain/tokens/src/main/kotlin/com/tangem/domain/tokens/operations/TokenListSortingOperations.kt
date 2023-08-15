package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.Network
import java.math.BigDecimal

internal class TokenListSortingOperations(
    private val currencies: List<CryptoCurrencyStatus>,
    private val isAnyTokenLoading: Boolean,
    private val sortByBalance: Boolean,
) {

    constructor(
        tokenList: TokenList,
        sortByBalance: Boolean = tokenList.sortedBy == TokenList.SortType.BALANCE,
        isAnyTokenLoading: Boolean = tokenList.totalFiatBalance is TokenList.FiatBalance.Loading,
    ) : this(
        currencies = when (tokenList) {
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap { it.currencies }
            is TokenList.Ungrouped -> tokenList.currencies
            is TokenList.NotInitialized -> emptyList()
        },
        isAnyTokenLoading = isAnyTokenLoading,
        sortByBalance = sortByBalance,
    )

    fun getGroupedTokens(networks: Set<Network>): Either<Error, NonEmptyList<NetworkGroup>> = either {
        ensure(currencies.isNotEmpty()) { Error.EmptyTokens }
        val networksNes = ensureNotNull(networks.toNonEmptySetOrNull()) {
            Error.EmptyNetworks
        }

        if (sortByBalance) {
            groupAndSortTokensByBalance(networksNes)
        } else {
            groupTokens(networksNes)
        }
    }

    fun getTokens(): Either<Error, NonEmptyList<CryptoCurrencyStatus>> = either {
        val nonEmptyCurrencies = ensureNotNull(currencies.toNonEmptyListOrNull()) {
            Error.EmptyTokens
        }

        if (sortByBalance) sortTokensByBalance(nonEmptyCurrencies) else nonEmptyCurrencies
    }

    fun getSortType(): TokenList.SortType = if (sortByBalance) TokenList.SortType.BALANCE else TokenList.SortType.NONE

    private fun Raise<Error>.groupTokens(networks: NonEmptySet<Network>): NonEmptyList<NetworkGroup> {
        val groupedTokens = currencies
            .groupBy { it.currency.networkId }
            .map { (networkId, tokens) ->
                val network = ensureNotNull(networks.firstOrNull { it.id == networkId }) {
                    Error.NetworkNotFound(networkId)
                }

                NetworkGroup(
                    network = network,
                    currencies = ensureNotNull(tokens.toNonEmptyListOrNull()) { Error.EmptyTokens },
                )
            }
            .toNonEmptyListOrNull()

        return ensureNotNull(groupedTokens) { Error.EmptyTokens }
    }

    private fun Raise<Error>.groupAndSortTokensByBalance(networks: NonEmptySet<Network>): NonEmptyList<NetworkGroup> {
        val groupsWithSortedTokens = groupTokens(networks)
            .map { group ->
                val tokens = group.currencies as? NonEmptyList<CryptoCurrencyStatus>
                    ?: error("Tokens can not be empty here")
                group.copy(currencies = sortTokensByBalance(tokens))
            }

        return if (isAnyTokenLoading) {
            groupsWithSortedTokens
        } else {
            sortGroupsByBalance(groupsWithSortedTokens)
        }
    }

    private fun sortTokensByBalance(tokens: NonEmptyList<CryptoCurrencyStatus>): NonEmptyList<CryptoCurrencyStatus> {
        return if (isAnyTokenLoading) {
            tokens
        } else {
            tokens.sortedByDescending { it.value.fiatAmount ?: BigDecimal.ZERO }
                .toNonEmptyListOrNull()
                ?: error("Tokens can not be empty here")
        }
    }

    private fun sortGroupsByBalance(groupsWithSortedTokens: NonEmptyList<NetworkGroup>): NonEmptyList<NetworkGroup> {
        return groupsWithSortedTokens
            .sortedByDescending { group ->
                group.currencies.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO }
            }
            .toNonEmptyListOrNull()
            ?: error("Tokens can not be empty here")
    }

    sealed class Error {

        object EmptyTokens : Error()

        object EmptyNetworks : Error()

        data class NetworkNotFound(val networkId: Network.ID) : Error()
    }
}