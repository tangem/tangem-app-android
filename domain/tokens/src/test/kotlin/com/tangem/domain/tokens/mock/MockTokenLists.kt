package com.tangem.domain.tokens.mock

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.mock.MockNetworksGroups.failedNetworksGroups
import com.tangem.domain.tokens.mock.MockNetworksGroups.loadedNetworksGroups
import com.tangem.domain.tokens.mock.MockNetworksGroups.sortedNetworksGroups
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokenLists {

    const val isGrouped = false
    const val isSortedByBalance = false

    val notInitializedTokenList = TokenList.NotInitialized

    val emptyGroupedTokenList = TokenList.GroupedByNetwork(
        groups = emptySet(),
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val emptyUngroupedTokenList = TokenList.Ungrouped(
        tokens = emptySet(),
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val failedGroupedTokenList = TokenList.GroupedByNetwork(
        groups = failedNetworksGroups,
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val failedUngroupedTokenList = TokenList.Ungrouped(
        tokens = MockTokensStates.failedTokenStates,
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val loadingUngroupedTokenList = with(failedUngroupedTokenList) {
        copy(
            tokens = tokens.map { it.copy(value = TokenStatus.Loading) }.toNonEmptySetOrNull()!!,
            totalFiatBalance = TokenList.FiatBalance.Loading,
        )
    }

    val loadingGroupedTokenList = with(failedGroupedTokenList) {
        copy(
            totalFiatBalance = TokenList.FiatBalance.Loading,
            groups = groups.map { group ->
                group.copy(
                    tokens = group.tokens
                        .map { it.copy(value = TokenStatus.Loading) }
                        .toNonEmptySetOrNull()!!,
                )
            }.toNonEmptySetOrNull()!!,
        )
    }

    val unsortedUngroupedTokenList: TokenList.Ungrouped
        get() {
            val tokens = MockTokensStates.loadedTokensStates

            return failedUngroupedTokenList.copy(
                tokens = tokens,
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
                        .flatMap { it.tokens as NonEmptySet<TokenStatus> }
                        .sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                ),
            )
        }

    val sortedUngroupedTokenList: TokenList.Ungrouped
        get() {
            val tokens = MockTokensStates.loadedTokensStates
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptySetOrNull()!!

            return unsortedUngroupedTokenList.copy(
                tokens = tokens,
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