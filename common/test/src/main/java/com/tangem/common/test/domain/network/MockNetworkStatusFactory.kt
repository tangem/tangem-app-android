package com.tangem.common.test.domain.network

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
object MockNetworkStatusFactory {

    private val defaultNetwork = MockCryptoCurrencyFactory().ethereum.network

    fun createVerified(
        network: Network = defaultNetwork,
        source: StatusSource = StatusSource.ACTUAL,
        transform: (NetworkStatus.Verified) -> NetworkStatus.Verified = { it },
    ): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = NetworkStatus.Verified(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                amounts = mapOf(),
                pendingTransactions = mapOf(),
                source = source,
            )
                .let(transform),
        )
    }

    fun createNoAccount(network: Network = defaultNetwork, source: StatusSource = StatusSource.ACTUAL): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = NetworkStatus.NoAccount(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x1",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "",
                source = source,
            ),
        )
    }

    fun createMissedDerivation(network: Network = defaultNetwork): NetworkStatus {
        return NetworkStatus(network = network, value = NetworkStatus.MissedDerivation)
    }

    fun createUnreachable(network: Network = defaultNetwork): NetworkStatus {
        return NetworkStatus(network = network, value = NetworkStatus.Unreachable(address = null))
    }
}