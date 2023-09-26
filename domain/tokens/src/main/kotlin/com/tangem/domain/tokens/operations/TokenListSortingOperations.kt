package com.tangem.domain.tokens.operations

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
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
            is TokenList.Empty -> emptyList()
        },
        isAnyTokenLoading = isAnyTokenLoading,
        sortByBalance = sortByBalance,
    )

    fun getGroupedTokens(): Either<Error, NonEmptyList<NetworkGroup>> = either {
        ensure(currencies.isNotEmpty()) { Error.EmptyTokens }

        if (sortByBalance) {
            groupAndSortTokensByBalance()
        } else {
            groupTokens()
        }
    }

    fun getTokens(): Either<Error, NonEmptyList<CryptoCurrencyStatus>> = either {
        val nonEmptyCurrencies = ensureNotNull(currencies.toNonEmptyListOrNull()) {
            Error.EmptyTokens
        }

        if (sortByBalance) sortTokensByBalance(nonEmptyCurrencies) else nonEmptyCurrencies
    }

    fun getSortType(): TokenList.SortType = if (sortByBalance) TokenList.SortType.BALANCE else TokenList.SortType.NONE

    private fun Raise<Error>.groupTokens(): NonEmptyList<NetworkGroup> {
        val groupedTokens = currencies
            .groupBy { it.currency.network }
            .map { (network, tokens) ->
                NetworkGroup(
                    network = network,
                    currencies = ensureNotNull(tokens.toNonEmptyListOrNull()) { Error.EmptyTokens },
                )
            }
            .toNonEmptyListOrNull()

        return ensureNotNull(groupedTokens) { Error.EmptyTokens }
    }

    private fun Raise<Error>.groupAndSortTokensByBalance(): NonEmptyList<NetworkGroup> {
        val groupsWithSortedTokens = groupTokens()
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

        data class NetworkNotFound(val networkId: Network.ID) : Error()
    }
}
