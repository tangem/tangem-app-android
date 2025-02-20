package com.tangem.domain.tokens.mock

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.mock.MockNetworksGroups.failedNetworksGroups
import com.tangem.domain.tokens.mock.MockNetworksGroups.loadedNetworksGroups
import com.tangem.domain.tokens.mock.MockNetworksGroups.sortedNetworksGroups
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TotalFiatBalance
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokenLists {

    const val isGrouped = false
    const val isSortedByBalance = false

    val emptyTokenList = TokenList.Empty

    val emptyGroupedTokenList = TokenList.GroupedByNetwork(
        groups = emptyList(),
        totalFiatBalance = TotalFiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val emptyUngroupedTokenList = TokenList.Ungrouped(
        currencies = emptyList(),
        totalFiatBalance = TotalFiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val failedGroupedTokenList = TokenList.GroupedByNetwork(
        groups = failedNetworksGroups,
        totalFiatBalance = TotalFiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val failedUngroupedTokenList = TokenList.Ungrouped(
        currencies = MockTokensStates.failedTokenStates,
        totalFiatBalance = TotalFiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val noQuotesUngroupedTokenList = failedUngroupedTokenList.copy(
        totalFiatBalance = TotalFiatBalance.Failed,
        currencies = MockTokensStates.noQuotesTokensStatuses,
    )

    val loadingUngroupedTokenList = with(failedUngroupedTokenList) {
        copy(
            currencies = currencies.map { it.copy(value = CryptoCurrencyStatus.Loading) }.toNonEmptyListOrNull()
                ?: emptyList(),
            totalFiatBalance = TotalFiatBalance.Loading,
        )
    }

    val loadingGroupedTokenList = with(failedGroupedTokenList) {
        copy(
            totalFiatBalance = TotalFiatBalance.Loading,
            groups = groups.map { group ->
                group.copy(
                    currencies = group.currencies
                        .map { it.copy(value = CryptoCurrencyStatus.Loading) }
                        .toNonEmptyListOrNull()
                        ?: emptyList(),
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
                totalFiatBalance = TotalFiatBalance.Loaded(
                    amount = tokens.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                    source = StatusSource.ACTUAL,
                ),
            )
        }

    val unsortedGroupedTokenList: TokenList.GroupedByNetwork
        get() {
            val groups = loadedNetworksGroups

            return failedGroupedTokenList.copy(
                groups = groups,
                sortedBy = TokenList.SortType.NONE,
                totalFiatBalance = TotalFiatBalance.Loaded(
                    amount = groups
                        .flatMap { it.currencies as NonEmptyList<CryptoCurrencyStatus> }
                        .sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                    source = StatusSource.ACTUAL,
                ),
            )
        }

    val sortedUngroupedTokenList: TokenList.Ungrouped
        get() {
            val tokens = MockTokensStates.loadedTokensStates
                .sortedByDescending { it.value.fiatAmount }

            return unsortedUngroupedTokenList.copy(
                currencies = tokens,
                sortedBy = TokenList.SortType.BALANCE,
            )
        }

    val sortedGroupedTokenList: TokenList.GroupedByNetwork
        get() {
            val groups = sortedNetworksGroups.toNonEmptyList()

            return unsortedGroupedTokenList.copy(
                groups = groups,
                sortedBy = TokenList.SortType.BALANCE,
            )
        }
}