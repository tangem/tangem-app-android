package com.tangem.domain.tokens.operations

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.NetworkStatus.Amount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.StakingIntegrationID
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
        fiatRateUSD = 1800.0.toBigDecimal(),
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                        mockk<BalanceItem>(relaxed = true) {
                            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
                        },
                        mockk<BalanceItem>(relaxed = true) {
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
                contributionsInput = BalanceContributionsInput.Disabled,
            )

            // Assert
            val expected = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Custom(
                    amount = BigDecimal.TEN,
                    fiatAmount = BigDecimal.TEN * fullQuote.fiatRate,
                    fiatRate = fullQuote.fiatRate,
                    priceChange = fullQuote.priceChange,
                    // the factory also stamps the per-network rule — Cardano keeps its stake in the network balance
                    stakingBalance = stakeKitBalance.copy(
                        balance = stakeKitBalance.balance.copy(
                            items = stakeKitBalance.balance.items.subList(0, 1),
                        ),
                        isStakedIncludedInNetworkBalance = true,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                        mockk<BalanceItem>(relaxed = true) {
                            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
                        },
                        mockk<BalanceItem>(relaxed = true) {
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
                contributionsInput = BalanceContributionsInput.Disabled,
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.TEN,
                // the factory also stamps the per-network rule — Cardano keeps its stake in the network balance
                stakingBalance = stakeKitBalance.copy(
                    balance = stakeKitBalance.balance.copy(
                        items = stakeKitBalance.balance.items.subList(0, 1),
                    ),
                    isStakedIncludedInNetworkBalance = true,
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
                contributionsInput = BalanceContributionsInput.Disabled,
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
                        mockk<BalanceItem>(relaxed = true) {
                            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
                        },
                        mockk<BalanceItem>(relaxed = true) {
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
                contributionsInput = BalanceContributionsInput.Disabled,
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN * fullQuote.fiatRate,
                fiatRate = fullQuote.fiatRate,
                priceChange = fullQuote.priceChange,
                // the factory also stamps the per-network rule — Cardano keeps its stake in the network balance
                stakingBalance = stakeKitBalance.copy(
                    balance = stakeKitBalance.balance.copy(
                        items = stakeKitBalance.balance.items.subList(0, 1),
                    ),
                    isStakedIncludedInNetworkBalance = true,
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

    /**
     * Legacy path (toggle off): the factory itself resolves the per-network "is the stake already inside the
     * network balance?" rule from the currency and stamps it on the attached balance, and `contributions` stays
     * empty so every reader falls back to the typed field.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContributionStamp {

        @Test
        fun `GIVEN Cardano currency WHEN create THEN stake is marked as included in the network balance`() {
            // Arrange
            val cardano = cryptoCurrencyFactory.cardano

            // Act
            val actual = createWithStaking(currency = cardano)

            // Assert
            val stakingBalance = actual.value.stakingBalance as StakingBalance.Data
            Truth.assertThat(stakingBalance.isStakedIncludedInNetworkBalance).isTrue()
            Truth.assertThat(stakingBalance.totalDeltaCryptoAmount()).isEqualTo(REWARDS_AMOUNT)
            Truth.assertThat(actual.value.contributions).isEmpty()
        }

        @Test
        fun `GIVEN Solana currency WHEN create THEN stake is not marked as included`() {
            // Arrange
            val solana = cryptoCurrencyFactory.createCoin(Blockchain.Solana)

            // Act
            val actual = createWithStaking(currency = solana)

            // Assert
            val stakingBalance = actual.value.stakingBalance as StakingBalance.Data
            Truth.assertThat(stakingBalance.isStakedIncludedInNetworkBalance).isFalse()
            Truth.assertThat(stakingBalance.totalDeltaCryptoAmount()).isEqualTo(STAKED_AMOUNT + REWARDS_AMOUNT)
            Truth.assertThat(actual.value.contributions).isEmpty()
        }

        @Test
        fun `GIVEN no staking balance WHEN create THEN there is no staking balance and no contributions`() {
            // Act
            val actual = createWithStaking(
                currency = cryptoCurrencyFactory.cardano,
                maybeStakingBalance = none(),
            )

            // Assert
            Truth.assertThat(actual.value.stakingBalance).isNull()
            Truth.assertThat(actual.value.contributions).isEmpty()
        }

        @Test
        fun `GIVEN P2PEthPool balance on Cardano WHEN create THEN the stake counts as already included`() {
            // Act
            val actual = createWithStaking(
                currency = cryptoCurrencyFactory.cardano,
                maybeStakingBalance = p2pEthPoolBalance().some(),
            )

            // Assert
            val stakingBalance = actual.value.stakingBalance as StakingBalance.Data
            Truth.assertThat(stakingBalance.isStakedIncludedInNetworkBalance).isTrue()
            Truth.assertThat(stakingBalance.totalDeltaCryptoAmount()).isEqualTo(REWARDS_AMOUNT)
        }

        @Test
        fun `GIVEN P2PEthPool balance on Ethereum WHEN create THEN stake is not marked as included`() {
            // Act
            val actual = createWithStaking(
                currency = cryptoCurrencyFactory.ethereum,
                maybeStakingBalance = p2pEthPoolBalance().some(),
            )

            // Assert
            val stakingBalance = actual.value.stakingBalance as StakingBalance.Data
            Truth.assertThat(stakingBalance.isStakedIncludedInNetworkBalance).isFalse()
            Truth.assertThat(stakingBalance.totalDeltaCryptoAmount()).isEqualTo(STAKED_AMOUNT + REWARDS_AMOUNT)
        }

        @Test
        fun `GIVEN StakeKit balance staked from another address WHEN create THEN nothing is attached`() {
            // Arrange
            val cardano = cryptoCurrencyFactory.cardano
            val elsewhere = stakeKitBalance(cardano).copy(
                stakingId = StakingID(integrationId = "integration", address = "0xanother"),
            )

            // Act
            val actual = createWithStaking(currency = cardano, maybeStakingBalance = elsewhere.some())

            // Assert — the stake belongs to a different address of the same wallet, not to this status
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @Test
        fun `GIVEN P2PEthPool balance staked from another address WHEN create THEN nothing is attached`() {
            // Act
            val actual = createWithStaking(
                currency = cryptoCurrencyFactory.ethereum,
                maybeStakingBalance = p2pEthPoolBalance(address = "0xanother").some(),
            )

            // Assert
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @Test
        fun `GIVEN StakeKit balance with no items of this currency WHEN create THEN nothing is attached`() {
            // Arrange — a balance whose every item belongs to another token, so the coinGeckoId filter empties it
            val cardano = cryptoCurrencyFactory.cardano
            val foreign = stakeKitBalance(currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana))

            // Act
            val actual = createWithStaking(currency = cardano, maybeStakingBalance = foreign.some())

            // Assert
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @Test
        fun `GIVEN an empty staking balance WHEN create THEN nothing is attached`() {
            // Act
            val actual = createWithStaking(
                currency = cryptoCurrencyFactory.cardano,
                maybeStakingBalance = StakingBalance.Empty(
                    stakingId = StakingID(integrationId = "integration", address = "0x123"),
                    source = StatusSource.ACTUAL,
                ).some(),
            )

            // Assert — only Data variants ever reach a status; this is what `stakingBalanceData` relies on
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @Test
        fun `GIVEN a failed staking balance WHEN create THEN nothing is attached`() {
            // Act
            val actual = createWithStaking(
                currency = cryptoCurrencyFactory.cardano,
                maybeStakingBalance = StakingBalance.Error(
                    stakingId = StakingID(integrationId = "integration", address = "0x123"),
                ).some(),
            )

            // Assert
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        private fun createWithStaking(
            currency: CryptoCurrency,
            maybeStakingBalance: Option<StakingBalance> = stakeKitBalance(currency).some(),
        ): CryptoCurrencyStatus {
            return create(
                currency = currency,
                maybeStakingBalance = maybeStakingBalance,
                useContributions = false,
                contributions = emptyList(),
            )
        }
    }

    /**
     * Contributions path (toggle on): the extra balances arrive already resolved from the seam's
     * `BalanceContributionProvider`s, so the factory neither matches nor stamps anything — it stores the list, and
     * back-fills the typed `stakingBalance` from it so the Axis-2 readers keep working until they downcast
     * contributions themselves.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContributionsFromSeam {

        @Test
        fun `GIVEN staking contribution WHEN create THEN it is stored and the typed field stays null`() {
            // Arrange
            val currency = cryptoCurrencyFactory.cardano
            val contribution = stakeKitBalance(currency).copy(isStakedIncludedInNetworkBalance = true)

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(contribution),
            )

            // Assert — the two paths are mutually exclusive: on this one the contribution is the only source, and
            // Axis-2 readers reach it through `stakingBalanceData` rather than through the typed field
            Truth.assertThat(actual.value.contributions).containsExactly(contribution)
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @Test
        fun `GIVEN a contribution that is not staking WHEN create THEN the typed field stays null`() {
            // Arrange — yield supply contributes zero and has no typed field to back-fill
            val currency = cryptoCurrencyFactory.cardano
            val contribution = YieldSupplyContribution(
                status = YieldSupplyStatus(
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = BigDecimal.ONE,
                ),
                source = StatusSource.ACTUAL,
            )

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(contribution),
            )

            // Assert
            Truth.assertThat(actual.value.contributions).containsExactly(contribution)
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @Test
        fun `GIVEN cached contribution WHEN create THEN its source degrades the total source`() {
            // Arrange
            val currency = cryptoCurrencyFactory.cardano
            val contribution = stakeKitBalance(currency).copy(source = StatusSource.ONLY_CACHE)

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(contribution),
            )

            // Assert
            Truth.assertThat(actual.value.sources.contributionSources).containsExactly(StatusSource.ONLY_CACHE)
            Truth.assertThat(actual.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        }

        @Test
        fun `GIVEN a custom token with a contribution WHEN create THEN its source lands in contributionSources`() {
            // Arrange — the Custom shape is built by a different factory branch than Loaded
            val custom = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Cardano).copy(isCustom = true)
            val contribution = stakeKitBalance(custom).copy(source = StatusSource.ONLY_CACHE)

            // Act
            val actual = create(
                currency = custom,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(contribution),
            )

            // Assert
            Truth.assertThat(actual.value).isInstanceOf(CryptoCurrencyStatus.Custom::class.java)
            Truth.assertThat(actual.value.contributions).containsExactly(contribution)
            Truth.assertThat(actual.value.sources.contributionSources).containsExactly(StatusSource.ONLY_CACHE)
            Truth.assertThat(actual.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        }

        @Test
        fun `GIVEN no quote and a contribution WHEN create THEN its source lands in contributionSources`() {
            // Arrange — the NoQuote shape is built by a third factory branch
            val currency = cryptoCurrencyFactory.cardano
            val contribution = stakeKitBalance(currency).copy(source = StatusSource.ONLY_CACHE)

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(contribution),
                maybeQuoteStatus = emptyQuoteStatus.some(),
            )

            // Assert
            Truth.assertThat(actual.value).isInstanceOf(CryptoCurrencyStatus.NoQuote::class.java)
            Truth.assertThat(actual.value.contributions).containsExactly(contribution)
            Truth.assertThat(actual.value.sources.contributionSources).containsExactly(StatusSource.ONLY_CACHE)
            Truth.assertThat(actual.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        }

        @Test
        fun `GIVEN a cache-fresh contribution WHEN create THEN the total source is cache and not only-cache`() {
            // Arrange — CACHE is the middle branch of getResultStatusSource; ONLY_CACHE alone cannot reach it
            val currency = cryptoCurrencyFactory.cardano
            val contribution = stakeKitBalance(currency).copy(source = StatusSource.CACHE)

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(contribution),
            )

            // Assert
            Truth.assertThat(actual.value.sources.contributionSources).containsExactly(StatusSource.CACHE)
            Truth.assertThat(actual.value.sources.total).isEqualTo(StatusSource.CACHE)
        }

        @Test
        fun `GIVEN several contributions WHEN create THEN every source is reported`() {
            // Arrange
            val currency = cryptoCurrencyFactory.cardano
            val fresh = stakeKitBalance(currency).copy(source = StatusSource.ACTUAL)
            val stale = YieldSupplyContribution(status = mockk(relaxed = true), source = StatusSource.ONLY_CACHE)

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(fresh, stale),
            )

            // Assert — one source per contribution, in order, and the worst of them wins the total
            Truth.assertThat(actual.value.sources.contributionSources)
                .containsExactly(StatusSource.ACTUAL, StatusSource.ONLY_CACHE)
                .inOrder()
            Truth.assertThat(actual.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        }

        @Test
        fun `GIVEN a legacy staking balance WHEN toggle is on THEN it is ignored`() {
            // Arrange — with the toggle on, only what the seam supplies counts
            val currency = cryptoCurrencyFactory.cardano

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = stakeKitBalance(currency).some(),
                useContributions = true,
                contributions = emptyList(),
            )

            // Assert
            Truth.assertThat(actual.value.contributions).isEmpty()
            Truth.assertThat(actual.value.stakingBalance).isNull()
        }

        @ParameterizedTest
        @MethodSource("provideNonVerifiedModels")
        fun `GIVEN contributions WHEN the network is not verified THEN the shape carries none of them`(
            model: NonVerifiedModel,
        ) {
            // Arrange — the seam supplies contributions regardless of the network status it ends up paired with
            val currency = cryptoCurrencyFactory.cardano

            // Act
            val actual = create(
                currency = currency,
                maybeStakingBalance = none(),
                useContributions = true,
                contributions = listOf(stakeKitBalance(currency)),
                maybeNetworkStatus = model.networkStatus,
            )

            // Assert — these shapes have no balance to fold into, so a contribution must not leak into them
            Truth.assertThat(actual.value).isInstanceOf(model.expectedShape)
            Truth.assertThat(actual.value.contributions).isEmpty()
            Truth.assertThat(actual.value.sources.contributionSources).isEmpty()
        }

        private fun provideNonVerifiedModels() = listOf(
            NonVerifiedModel(
                description = "no network status -> Loading",
                networkStatus = none(),
                expectedShape = CryptoCurrencyStatus.Loading::class.java,
            ),
            NonVerifiedModel(
                description = "MissedDerivation",
                networkStatus = NetworkStatus.MissedDerivation.toStatus().some(),
                expectedShape = CryptoCurrencyStatus.MissedDerivation::class.java,
            ),
            NonVerifiedModel(
                description = "Unreachable",
                networkStatus = NetworkStatus.Unreachable(address = networkAddress).toStatus().some(),
                expectedShape = CryptoCurrencyStatus.Unreachable::class.java,
            ),
            NonVerifiedModel(
                description = "NoAccount",
                networkStatus = NetworkStatus.NoAccount(
                    address = networkAddress,
                    amountToCreateAccount = BigDecimal.ONE,
                    errorMessage = "error message",
                    source = StatusSource.ACTUAL,
                ).toStatus().some(),
                expectedShape = CryptoCurrencyStatus.NoAccount::class.java,
            ),
            NonVerifiedModel(
                description = "Verified but the amount was not found -> NoAmount",
                networkStatus = createVerified(amounts = mapOf(currency.id to Amount.NotFound))
                    .toStatus()
                    .some(),
                expectedShape = CryptoCurrencyStatus.NoAmount::class.java,
            ),
        )
    }

    data class NonVerifiedModel(
        val description: String,
        val networkStatus: Option<NetworkStatus>,
        val expectedShape: Class<out CryptoCurrencyStatus.Value>,
    ) {

        override fun toString(): String = description
    }

    // region Extra-balance fixtures shared by both paths
    private fun create(
        currency: CryptoCurrency,
        maybeStakingBalance: Option<StakingBalance>,
        useContributions: Boolean,
        contributions: List<BalanceContribution>,
        maybeQuoteStatus: Option<QuoteStatus> =
            QuoteStatus(rawCurrencyId = currency.id.rawCurrencyId!!, value = fullQuote).some(),
        maybeNetworkStatus: Option<NetworkStatus> = NetworkStatus(
            network = currency.network,
            value = createVerified(amounts = mapOf(currency.id to Amount.Loaded(value = BigDecimal.TEN))),
        ).some(),
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatusFactory.create(
            currency = currency,
            maybeNetworkStatus = maybeNetworkStatus,
            maybeQuoteStatus = maybeQuoteStatus,
            maybeStakingBalance = maybeStakingBalance,
            contributionsInput = if (useContributions) {
                BalanceContributionsInput.enabled(contributions)
            } else {
                BalanceContributionsInput.Disabled
            },
        )
    }

    /**
     * Staked / rewards only — the unstaking and withdrawable buckets are derived from the exit queue, and the
     * arithmetic over all four is already pinned in `:domain:models` by `StakingBalanceContributionTest`. Here only
     * the factory's *stamping* of the per-network rule is under test.
     */
    private fun p2pEthPoolBalance(
        address: String = networkAddress.defaultAddress.value,
    ): StakingBalance.Data.P2PEthPool {
        return StakingBalance.Data.P2PEthPool(
            stakingId = StakingID(integrationId = "integration", address = address),
            source = StatusSource.ACTUAL,
            accounts = listOf(
                P2PEthPoolStakingAccount(
                    delegatorAddress = "0xdelegator",
                    vaultAddress = "0xvault",
                    stake = P2PEthPoolStake(assets = STAKED_AMOUNT, totalEarnedAssets = REWARDS_AMOUNT),
                    availableToUnstake = BigDecimal.ZERO,
                    availableToWithdraw = BigDecimal.ZERO,
                    exitQueue = P2PEthPoolExitQueue(total = BigDecimal.ZERO, requests = emptyList()),
                ),
            ),
        )
    }

    private fun stakeKitBalance(currency: CryptoCurrency): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = StakingID(
                integrationId = "integration",
                address = networkAddress.defaultAddress.value,
            ),
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = listOf(
                    balanceItem(currency = currency, amount = STAKED_AMOUNT, type = BalanceType.STAKED),
                    balanceItem(currency = currency, amount = REWARDS_AMOUNT, type = BalanceType.REWARDS),
                ),
                integrationId = "integration",
            ),
        )
    }

    private fun balanceItem(currency: CryptoCurrency, amount: BigDecimal, type: BalanceType): BalanceItem {
        return mockk(relaxed = true) {
            every { this@mockk.token.coinGeckoId } returns currency.id.rawCurrencyId?.value
            every { this@mockk.amount } returns amount
            every { this@mockk.type } returns type
        }
    }
    // endregion

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

    private companion object {

        val STAKED_AMOUNT: BigDecimal = BigDecimal(9)
        val REWARDS_AMOUNT: BigDecimal = BigDecimal.ONE
    }
}