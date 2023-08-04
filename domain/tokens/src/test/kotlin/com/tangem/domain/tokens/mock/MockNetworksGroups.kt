package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.model.NetworkGroup

@Suppress("MemberVisibilityCanBePrivate")
internal object MockNetworksGroups {

    val networkGroup1 = NetworkGroup(
        network = MockNetworks.network1,
        currencies = MockTokensStates.failedTokenStates
            .filter { it.currency.networkId == MockNetworks.network1.id }
            .toNonEmptySetOrNull()!!,
    )

    val networkGroup2 = NetworkGroup(
        network = MockNetworks.network2,
        currencies = MockTokensStates.failedTokenStates
            .filter { it.currency.networkId == MockNetworks.network2.id }
            .toNonEmptySetOrNull()!!,
    )

    val networkGroup3 = NetworkGroup(
        network = MockNetworks.network3,
        currencies = MockTokensStates.failedTokenStates
            .filter { it.currency.networkId == MockNetworks.network3.id }
            .toNonEmptySetOrNull()!!,
    )

    val failedNetworksGroups = nonEmptySetOf(networkGroup1, networkGroup2, networkGroup3)

    val loadedNetworksGroups = failedNetworksGroups.map { group ->
        group.copy(
            currencies = MockTokensStates.loadedTokensStates
                .filter { it.currency.networkId == group.network.id }
                .toNonEmptySetOrNull()!!,
        )
    }.toNonEmptySet()

    val sortedNetworksGroups = loadedNetworksGroups.map { group ->
        group.copy(
            currencies = group.currencies
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptySetOrNull()!!,
        )
    }
        .sortedByDescending { group ->
            group.currencies.sumOf { it.value.fiatAmount!! }
        }
        .toNonEmptySetOrNull()!!
}