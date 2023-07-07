package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokenLists {

    const val isGrouped = false
    const val isSortedByBalance = false

    val networkGroup1 = NetworkGroup(
        networkId = MockNetworks.network1.id,
        name = MockNetworks.network1.name,
        tokens = MockTokensStates.tokenStates
            .filter { it.networkId == MockNetworks.network1.id }
            .toNonEmptySetOrNull()!!,
    )

    val networkGroup2 = NetworkGroup(
        networkId = MockNetworks.network2.id,
        name = MockNetworks.network2.name,
        tokens = MockTokensStates.tokenStates
            .filter { it.networkId == MockNetworks.network2.id }
            .toNonEmptySetOrNull()!!,
    )

    val networkGroup3 = NetworkGroup(
        networkId = MockNetworks.network3.id,
        name = MockNetworks.network3.name,
        tokens = MockTokensStates.tokenStates
            .filter { it.networkId == MockNetworks.network3.id }
            .toNonEmptySetOrNull()!!,
    )

    val networksGroups = nonEmptySetOf(networkGroup1, networkGroup2, networkGroup3)

    val sortedNetworksGroups = networksGroups.map { group ->
        group.copy(
            tokens = MockTokensStates.loadedTokensStates
                .filter { it.networkId == group.networkId }
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptySetOrNull()!!,
        )
    }
        .sortedByDescending { group ->
            group.tokens.sumOf { it.value.fiatAmount!! }
        }
        .toNonEmptySetOrNull()!!

    val notInitializedTokenList = TokenList.NotInitialized

    val groupedTokenList = TokenList.GroupedByNetwork(
        groups = networksGroups,
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val ungroupedTokenList = TokenList.Ungrouped(
        tokens = MockTokensStates.tokenStates,
        totalFiatBalance = TokenList.FiatBalance.Failed,
        sortedBy = TokenList.SortType.NONE,
    )

    val loadingUngroupedTokenList = with(ungroupedTokenList) {
        copy(
            tokens = tokens.map { it.copy(value = TokenStatus.Loading) },
            totalFiatBalance = TokenList.FiatBalance.Loading,
        )
    }

    val sortedUngroupedTokenList: TokenList.Ungrouped
        get() {
            val tokens = MockTokensStates.loadedTokensStates
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptySetOrNull()!!

            return ungroupedTokenList.copy(
                tokens = tokens,
                sortedBy = TokenList.SortType.BALANCE,
                totalFiatBalance = TokenList.FiatBalance.Loaded(
                    amount = tokens.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                ),
            )
        }

    val sortedGroupedTokenList: TokenList.GroupedByNetwork
        get() {
            val groups = sortedNetworksGroups

            return groupedTokenList.copy(
                groups = groups,
                sortedBy = TokenList.SortType.BALANCE,
                totalFiatBalance = TokenList.FiatBalance.Loaded(
                    amount = groups
                        .flatMap { it.tokens }
                        .sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
                    isAllAmountsSummarized = true,
                ),
            )
        }
}
