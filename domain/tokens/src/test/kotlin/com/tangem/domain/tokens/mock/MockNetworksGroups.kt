package com.tangem.domain.tokens.mock

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.tokens.model.NetworkGroup

@Suppress("MemberVisibilityCanBePrivate")
internal object MockNetworksGroups {

    val networkGroup1 = NetworkGroup(
        network = MockNetworks.network1,
        currencies = MockTokensStates.failedTokenStates
            .filter { it.currency.networkId == MockNetworks.network1.id }
            .toNonEmptyListOrNull()!!,
    )

    val networkGroup2 = NetworkGroup(
        network = MockNetworks.network2,
        currencies = MockTokensStates.failedTokenStates
            .filter { it.currency.networkId == MockNetworks.network2.id }
            .toNonEmptyListOrNull()!!,
    )

    val networkGroup3 = NetworkGroup(
        network = MockNetworks.network3,
        currencies = MockTokensStates.failedTokenStates
            .filter { it.currency.networkId == MockNetworks.network3.id }
            .toNonEmptyListOrNull()!!,
    )

    val failedNetworksGroups = nonEmptyListOf(networkGroup1, networkGroup2, networkGroup3)

    val loadedNetworksGroups = failedNetworksGroups.map { group ->
        group.copy(
            currencies = MockTokensStates.loadedTokensStates
                .filter { it.currency.networkId == group.network.id }
                .toNonEmptyListOrNull()!!,
        )
    }

    val sortedNetworksGroups = loadedNetworksGroups.map { group ->
        group.copy(
            currencies = group.currencies
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptyListOrNull()!!,
        )
    }
        .sortedByDescending { group ->
            group.currencies.sumOf { it.value.fiatAmount!! }
        }
        .toNonEmptyListOrNull()!!
}