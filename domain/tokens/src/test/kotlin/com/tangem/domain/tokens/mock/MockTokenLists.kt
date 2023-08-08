package com.tangem.domain.tokens.mock

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.tokens.mock.MockNetworksGroups.failedNetworksGroups
import com.tangem.domain.tokens.mock.MockNetworksGroups.loadedNetworksGroups
import com.tangem.domain.tokens.mock.MockNetworksGroups.sortedNetworksGroups
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokenLists {

    const val isGrouped = false
    const val isSortedByBalance = false

    val notInitializedTokenList = TokenList.NotInitialized

    val emptyGroupedTokenList = TokenList.GroupedByNetwork(
        groups = emptyList(),
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val emptyUngroupedTokenList = TokenList.Ungrouped(
        currencies = emptyList(),
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val failedGroupedTokenList = TokenList.GroupedByNetwork(
        groups = failedNetworksGroups,
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val failedUngroupedTokenList = TokenList.Ungrouped(
        currencies = MockTokensStates.failedTokenStates,
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val loadingUngroupedTokenList = with(failedUngroupedTokenList) {
        copy(
            currencies = currencies.map { it.copy(value = CryptoCurrencyStatus.Loading) }.toNonEmptyListOrNull()!!,
            totalFiatBalance = TokenList.FiatBalance.Loading,
        )
    }

    val loadingGroupedTokenList = with(failedGroupedTokenList) {
        copy(
            totalFiatBalance = TokenList.FiatBalance.Loading,
            groups = groups.map { group ->
                group.copy(
                    currencies = group.currencies
                        .map { it.copy(value = CryptoCurrencyStatus.Loading) }
                        .toNonEmptyListOrNull()!!,
                )
            }.toNonEmptyListOrNull()!!,
        )
    }

    val unsortedUngroupedTokenList: TokenList.Ungrouped
        get() {
            val tokens = MockTokensStates.loadedTokensStates

            return failedUngroupedTokenList.copy(
                currencies = tokens,
                sortedBy = TokenList.SortType.NONE,
                totalFiatBalance = TokenList.FiatBalance.Loaded(
                    amount = tokens.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                ),
            )
        }

    val unsortedGroupedTokenList: TokenList.GroupedByNetwork
        get() {
            val groups = loadedNetworksGroups

            return failedGroupedTokenList.copy(
                groups = groups,
                sortedBy = TokenList.SortType.NONE,
                totalFiatBalance = TokenList.FiatBalance.Loaded(
                    amount = groups
                        .flatMap { it.currencies as NonEmptyList<CryptoCurrencyStatus> }
                        .sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                ),
            )
        }

    val sortedUngroupedTokenList: TokenList.Ungrouped
        get() {
            val tokens = MockTokensStates.loadedTokensStates
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptyListOrNull()!!

            return unsortedUngroupedTokenList.copy(
                currencies = tokens,
                sortedBy = TokenList.SortType.BALANCE,
            )
        }

    val sortedGroupedTokenList: TokenList.GroupedByNetwork
        get() {
            val groups = sortedNetworksGroups

            return unsortedGroupedTokenList.copy(
                groups = groups,
                sortedBy = TokenList.SortType.BALANCE,
            )
        }
}
