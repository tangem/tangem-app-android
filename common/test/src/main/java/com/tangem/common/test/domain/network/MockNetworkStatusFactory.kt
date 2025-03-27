package com.tangem.common.test.domain.network

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.NetworkStatus
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
object MockNetworkStatusFactory {

    private val defaultNetwork = MockCryptoCurrencyFactory().ethereum.network

    fun createVerified(network: Network = defaultNetwork): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = NetworkStatus.Verified(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x123",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                amounts = mapOf(),
                pendingTransactions = mapOf(),
                source = StatusSource.ACTUAL,
            ),
        )
    }

    fun createNoAccount(network: Network = defaultNetwork): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = NetworkStatus.NoAccount(
                address = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x123",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                amountToCreateAccount = BigDecimal.ONE,
                errorMessage = "",
                source = StatusSource.ACTUAL,
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