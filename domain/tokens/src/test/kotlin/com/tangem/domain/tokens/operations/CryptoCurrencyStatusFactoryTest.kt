package com.tangem.domain.tokens.operations

import arrow.core.none
import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.NetworkStatus.Amount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.StakingIntegrationID
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyStatusFactoryTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val currency = cryptoCurrencyFactory.cardano

    private val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(
            value = "0x123",
            type = NetworkAddress.Address.Type.Primary,
        ),
    )

    private val fullQuote = QuoteStatus.Data(
        fiatRate = 1800.0.toBigDecimal(),
        priceChange = (-2.5).toBigDecimal(),
        source = StatusSource.ACTUAL,
    )
    private val emptyQuoteStatus = QuoteStatus.Empty.toStatus()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MissedDerivation {

        private val networkStatus = NetworkStatus.MissedDerivation.toStatus()
        private val maybeStakingBalance = none<StakingBalance>() // not relevant for this test

        @Test
        fun `network is MissedDerivation and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.MissedDerivation(
                fiatRate = fullQuote.fiatRate,
                priceChange = fullQuote.priceChange,
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is MissedDerivation and QuoteStatus is Empty`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = emptyQuoteStatus.some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.MissedDerivation(fiatRate = null, priceChange = null).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is MissedDerivation and QuoteStatus is null`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = none(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.MissedDerivation(fiatRate = null, priceChange = null).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Unreachable {

        private val networkStatus = NetworkStatus.Unreachable(address = networkAddress).toStatus()

        private val maybeStakingBalance = none<StakingBalance>() // not relevant for this test

        @Test
        fun `network is Unreachable and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Unreachable(
                fiatRate = fullQuote.fiatRate,
                priceChange = fullQuote.priceChange,
                networkAddress = networkAddress,
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Unreachable and QuoteStatus is Empty`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = emptyQuoteStatus.some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Unreachable(
                fiatRate = null,
                priceChange = null,
                networkAddress = networkAddress,
            )
                .toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Unreachable and QuoteStatus is null`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = none(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Unreachable(
                fiatRate = null,
                priceChange = null,
                networkAddress = networkAddress,
            ).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoAccount {

        private val networkStatus = NetworkStatus.NoAccount(
            address = networkAddress,
            amountToCreateAccount = BigDecimal.ONE,
            errorMessage = "error message",
            source = StatusSource.ACTUAL,
        ).toStatus()

        private val maybeStakingBalance = none<StakingBalance>() // not relevant for this test

        @Test
        fun `network is NoAccount and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAccount(
                amountToCreateAccount = BigDecimal.ONE,
                fiatAmount = BigDecimal.ZERO,
                priceChange = fullQuote.priceChange,
                fiatRate = fullQuote.fiatRate,
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is NoAccount and QuoteStatus is Empty`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = emptyQuoteStatus.some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAccount(
                amountToCreateAccount = BigDecimal.ONE,
                fiatAmount = BigDecimal.ZERO,
                priceChange = null,
                fiatRate = null,
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is NoAccount and QuoteStatus is null`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = none(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAccount(
                amountToCreateAccount = BigDecimal.ONE,
                fiatAmount = BigDecimal.ZERO,
                priceChange = null,
                fiatRate = null,
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoAmount {

        private val networkStatus = createVerified(amounts = mapOf(currency.id to Amount.NotFound)).toStatus()

        private val maybeStakingBalance = none<StakingBalance>() // not relevant for this test

        @Test
        fun `network is Verified with Amount is NotFound and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAmount(
                priceChange = fullQuote.priceChange,
                fiatRate = fullQuote.fiatRate,
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified with Amount is NotFound and QuoteStatus is Empty`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = emptyQuoteStatus.some(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified with Amount is NotFound and QuoteStatus is null`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = none(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Loading {

        private val maybeStakingBalance = none<StakingBalance>() // not relevant for this test

        @Test
        fun `network is null`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = none(),
                maybeQuoteStatus = none(), // not relevant for this test,
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loading.toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified with Amount is null`() {
            // Arrange
            val networkStatus = createVerified().toStatus()

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = none(), // not relevant for this test,
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loading.toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified and QuoteStatus is null`() {
            // Arrange
            val networkStatus = createVerified(
                amounts = mapOf(currency.id to Amount.Loaded(value = BigDecimal.ZERO)),
            ).toStatus()

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = none(),
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loading.toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Custom {

        private val currency = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Cardano).copy(isCustom = true)

        private val networkStatus = createVerified(
            amounts = mapOf(currency.id to Amount.Loaded(value = BigDecimal.TEN)),
        ).toStatus()

        @Test
        fun `network is Verified, QuoteStatus is null, YieldBalance is null`() {
            // Arrange
            val maybeQuoteStatus = none<QuoteStatus>()
            val maybeStakingBalance = none<StakingBalance>()

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Custom(
                    amount = BigDecimal.TEN,
                    fiatAmount = null,
                    fiatRate = null,
                    priceChange = null,
                    stakingBalance = null,
                    yieldSupplyStatus = null,
                    hasCurrentNetworkTransactions = false,
                    pendingTransactions = emptySet(),
                    networkAddress = networkAddress,
                    sources = CryptoCurrencyStatus.Sources(),
                ),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified, QuoteStatus is Data, YieldBalance is Data`() {
            // Arrange
            val stakeKitBalance = StakingBalance.Data.StakeKit(
                stakingId = StakingID(
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value,
                    address = networkAddress.defaultAddress.value,
                ),
                source = StatusSource.ACTUAL,
                balance = YieldBalanceItem(
                    items = listOf(
                        mockk<BalanceItem> {
                            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
                        },
                        mockk<BalanceItem> {
                            every { this@mockk.token.coinGeckoId } returns "unknown"
                        },
                    ),
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value,
                ),
            )

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                maybeStakingBalance = stakeKitBalance.some(),
            )

            // Assert
            val expected = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Custom(
                    amount = BigDecimal.TEN,
                    fiatAmount = BigDecimal.TEN * fullQuote.fiatRate,
                    fiatRate = fullQuote.fiatRate,
                    priceChange = fullQuote.priceChange,
                    stakingBalance = stakeKitBalance.copy(
                        balance = stakeKitBalance.balance.copy(
                            items = stakeKitBalance.balance.items.subList(0, 1),
                        ),
                    ),
                    yieldSupplyStatus = null,
                    hasCurrentNetworkTransactions = false,
                    pendingTransactions = emptySet(),
                    networkAddress = networkAddress,
                    sources = CryptoCurrencyStatus.Sources(),
                ),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoQuote {

        private val pendingTransactions = mapOf(currency.id to setOf(mockk<TxInfo>()))
        private val yieldSupplyStatuses = mapOf(currency.id to mockk<YieldSupplyStatus>())

        private val networkStatus = createVerified(
            amounts = mapOf(currency.id to Amount.Loaded(value = BigDecimal.TEN)),
            pendingTransactions = pendingTransactions,
            yieldSupplyStatuses = yieldSupplyStatuses,
        ).toStatus()

        private val maybeQuoteStatus = emptyQuoteStatus.some()

        @Test
        fun `network is Verified and YieldBalance is null`() {
            // Arrange
            val maybeStakingBalance = none<StakingBalance>()

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.TEN,
                stakingBalance = null,
                yieldSupplyStatus = yieldSupplyStatuses[currency.id]!!,
                hasCurrentNetworkTransactions = true,
                pendingTransactions = pendingTransactions[currency.id]!!,
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified and YieldBalance is Data`() {
            // Arrange
            val stakeKitBalance = StakingBalance.Data.StakeKit(
                stakingId = StakingID(
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value,
                    address = networkAddress.defaultAddress.value,
                ),
                source = StatusSource.ACTUAL,
                balance = YieldBalanceItem(
                    items = listOf(
                        mockk<BalanceItem> {
                            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
                        },
                        mockk<BalanceItem> {
                            every { this@mockk.token.coinGeckoId } returns "unknown"
                        },
                    ),
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value,
                ),
            )

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                maybeStakingBalance = stakeKitBalance.some(),
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.TEN,
                stakingBalance = stakeKitBalance.copy(
                    balance = stakeKitBalance.balance.copy(
                        items = stakeKitBalance.balance.items.subList(0, 1),
                    ),
                ),
                yieldSupplyStatus = yieldSupplyStatuses[currency.id]!!,
                hasCurrentNetworkTransactions = true,
                pendingTransactions = pendingTransactions[currency.id]!!,
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Loaded {

        private val networkStatus = createVerified(
            amounts = mapOf(currency.id to Amount.Loaded(value = BigDecimal.TEN)),
        ).toStatus()

        private val maybeQuoteStatus = fullQuote.toStatus().some()

        @Test
        fun `network is Verified and YieldBalance is null`() {
            // Arrange
            val maybeStakingBalance = none<StakingBalance>()

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                maybeStakingBalance = maybeStakingBalance,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN * fullQuote.fiatRate,
                fiatRate = fullQuote.fiatRate,
                priceChange = fullQuote.priceChange,
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `network is Verified and YieldBalance is Data`() {
            // Arrange
            val stakeKitBalance = StakingBalance.Data.StakeKit(
                stakingId = StakingID(
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value,
                    address = networkAddress.defaultAddress.value,
                ),
                source = StatusSource.ACTUAL,
                balance = YieldBalanceItem(
                    items = listOf(
                        mockk<BalanceItem> {
                            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
                        },
                        mockk<BalanceItem> {
                            every { this@mockk.token.coinGeckoId } returns "unknown"
                        },
                    ),
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano.value,
                ),
            )

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                maybeStakingBalance = stakeKitBalance.some(),
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN * fullQuote.fiatRate,
                fiatRate = fullQuote.fiatRate,
                priceChange = fullQuote.priceChange,
                stakingBalance = stakeKitBalance.copy(
                    balance = stakeKitBalance.balance.copy(
                        items = stakeKitBalance.balance.items.subList(0, 1),
                    ),
                ),
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ).toStatus()

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    private fun createVerified(
        amounts: Map<CryptoCurrency.ID, Amount> = emptyMap(),
        pendingTransactions: Map<CryptoCurrency.ID, Set<TxInfo>> = emptyMap(),
        yieldSupplyStatuses: Map<CryptoCurrency.ID, YieldSupplyStatus?> = emptyMap(),
    ): NetworkStatus.Verified {
        return NetworkStatus.Verified(
            address = networkAddress,
            amounts = amounts,
            pendingTransactions = pendingTransactions,
            yieldSupplyStatuses = yieldSupplyStatuses,
            source = StatusSource.ACTUAL,
        )
    }

    private fun NetworkStatus.Value.toStatus(): NetworkStatus {
        return NetworkStatus(network = currency.network, value = this)
    }

    private fun CryptoCurrencyStatus.Value.toStatus(): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(currency = currency, value = this)
    }

    private fun QuoteStatus.Value.toStatus(): QuoteStatus {
        return when (this) {
            is QuoteStatus.Data -> QuoteStatus(rawCurrencyId = currency.id.rawCurrencyId!!, value = this)
            is QuoteStatus.Empty -> QuoteStatus(rawCurrencyId = currency.id.rawCurrencyId!!, value = this)
        }
    }
}