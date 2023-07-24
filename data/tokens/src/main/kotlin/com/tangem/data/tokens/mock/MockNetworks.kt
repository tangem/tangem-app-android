package com.tangem.data.tokens.mock

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockNetworks {

    val network1 = Network(
        id = Network.ID("network1"),
        name = "Network One",
    )

    val network2 = Network(
        id = Network.ID("network2"),
        name = "Network Two",
    )

    val network3 = Network(
        id = Network.ID("network3"),
        name = "Network Three",
    )

    val networks = setOf(network1, network2, network3)

    val networkStatus1 = NetworkStatus(
        networkId = network1.id,
        value = NetworkStatus.Verified(
            amounts = mapOf(
                MockTokens.token1.id to BigDecimal("123.1234556789"),
                MockTokens.token2.id to BigDecimal("42.2"),
                MockTokens.token3.id to BigDecimal("1000000000.5"),
            ),
        ),
    )

    val networkStatus2 = NetworkStatus(
        networkId = network2.id,
        value = NetworkStatus.MissedDerivation,
    )

    val networkStatus3 = NetworkStatus(
        networkId = network3.id,
        value = NetworkStatus.Verified(
            amounts = mapOf(
                MockTokens.token7.id to BigDecimal.ZERO,
                MockTokens.token8.id to BigDecimal.TEN,
                MockTokens.token9.id to BigDecimal.TEN,
                MockTokens.token10.id to BigDecimal.TEN,
            ),
        ),
    )

    val networksStatuses = setOf(networkStatus1, networkStatus2, networkStatus3)
}