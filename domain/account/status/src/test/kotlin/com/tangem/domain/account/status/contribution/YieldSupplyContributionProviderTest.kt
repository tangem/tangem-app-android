package com.tangem.domain.account.status.contribution

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Yield supply has no data source of its own — it projects out of the network status the producer hands to the
 * resolver, and always contributes zero to a total.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyContributionProviderTest {

    private val provider = YieldSupplyContributionProvider()

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val currency = cryptoCurrencyFactory.ethereum
    private val wallet = MockUserWalletFactory.create()

    @Test
    fun `GIVEN yield status for the currency WHEN resolve THEN a zero-delta contribution with the network source`() =
        runTest {
            // Arrange
            val status = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            )

            // Act
            val actual = provider.contributions(wallet).first().resolve(
                currency = currency,
                networkStatus = verifiedStatus(yieldSupplyStatuses = mapOf(currency.id to status)),
            )

            // Assert
            assertThat(actual).isEqualTo(
                YieldSupplyContribution(status = status, source = StatusSource.ONLY_CACHE),
            )
            assertThat(actual?.totalDeltaCryptoAmount()).isEqualTo(BigDecimal.ZERO)
            assertThat(actual?.kind).isEqualTo(YieldSupplyContribution.CONTRIBUTION_KIND)
        }

    @Test
    fun `GIVEN no yield status for the currency WHEN resolve THEN nothing is contributed`() = runTest {
        // Act
        val actual = provider.contributions(wallet).first().resolve(
            currency = currency,
            networkStatus = verifiedStatus(yieldSupplyStatuses = emptyMap()),
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN unreachable network WHEN resolve THEN nothing is contributed`() = runTest {
        // Arrange
        val unreachable = NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Unreachable(address = address()),
        )

        // Act
        val actual = provider.contributions(wallet).first().resolve(currency, unreachable)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN no network status WHEN resolve THEN nothing is contributed`() = runTest {
        // Act
        val actual = provider.contributions(wallet).first().resolve(currency, networkStatus = null)

        // Assert
        assertThat(actual).isNull()
    }

    // region Fixtures
    private fun verifiedStatus(
        yieldSupplyStatuses: Map<CryptoCurrency.ID, YieldSupplyStatus?>,
    ): NetworkStatus {
        return NetworkStatus(
            network = currency.network,
            value = NetworkStatus.Verified(
                address = address(),
                amounts = emptyMap(),
                pendingTransactions = emptyMap(),
                yieldSupplyStatuses = yieldSupplyStatuses,
                // a non-ACTUAL source proves the contribution reports the network's freshness, not a default
                source = StatusSource.ONLY_CACHE,
            ),
        )
    }

    private fun address() = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
    )
    // endregion
}