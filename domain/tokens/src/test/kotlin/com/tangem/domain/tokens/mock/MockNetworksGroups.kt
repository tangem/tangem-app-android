package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.model.NetworkGroup

@Suppress("MemberVisibilityCanBePrivate")
internal object MockNetworksGroups {

    val networkGroup1 = NetworkGroup(
        network = MockNetworks.network1,
        tokens = MockTokensStates.failedTokenStates
            .filter { it.networkId == MockNetworks.network1.id }
            .toNonEmptySetOrNull()!!,
    )

    val networkGroup2 = NetworkGroup(
        network = MockNetworks.network2,
        tokens = MockTokensStates.failedTokenStates
            .filter { it.networkId == MockNetworks.network2.id }
            .toNonEmptySetOrNull()!!,
    )

    val networkGroup3 = NetworkGroup(
        network = MockNetworks.network3,
        tokens = MockTokensStates.failedTokenStates
            .filter { it.networkId == MockNetworks.network3.id }
            .toNonEmptySetOrNull()!!,
    )

    val failedNetworksGroups = nonEmptySetOf(networkGroup1, networkGroup2, networkGroup3)

    val loadedNetworksGroups = failedNetworksGroups.map { group ->
        group.copy(
            tokens = MockTokensStates.loadedTokensStates
                .filter { it.networkId == group.network.id }
                .toNonEmptySetOrNull()!!,
        )
    }.toNonEmptySet()

    val sortedNetworksGroups = loadedNetworksGroups.map { group ->
        group.copy(
            tokens = group.tokens
                .sortedByDescending { it.value.fiatAmount }
                .toNonEmptySetOrNull()!!,
        )
    }
        .sortedByDescending { group ->
            group.tokens.sumOf { it.value.fiatAmount!! }
        }
        .toNonEmptySetOrNull()!!
}