package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
object MockNetworks {

    val amountToCreateAccount: BigDecimal = BigDecimal.TEN

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

    val networks = nonEmptySetOf(network1, network2, network3)

    val networkStatus1 = NetworkStatus(
        networkId = network1.id,
        value = NetworkStatus.Unreachable,
    )

    val networkStatus2 = NetworkStatus(
        networkId = network2.id,
        value = NetworkStatus.MissedDerivation,
    )

    val networkStatus3 = NetworkStatus(
        networkId = network3.id,
        value = NetworkStatus.NoAccount(
            amountToCreateAccount = amountToCreateAccount,
        ),
    )

    val networksStatuses = nonEmptySetOf(networkStatus1, networkStatus2, networkStatus3)
}
