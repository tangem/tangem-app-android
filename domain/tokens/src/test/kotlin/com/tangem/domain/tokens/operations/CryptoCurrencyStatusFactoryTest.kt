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
import com.tangem.domain.staking.model.BalanceItem
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.models.yield.supply.yieldSupply
import com.tangem.test.core.ProvideTestModels
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
        @Test
        fun `network is MissedDerivation and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                contributions = emptyList(),
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
                contributions = emptyList(),
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
                contributions = emptyList(),
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

        @Test
        fun `network is Unreachable and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                contributions = emptyList(),
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
                contributions = emptyList(),
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
                contributions = emptyList(),
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

        @Test
        fun `network is NoAccount and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                contributions = emptyList(),
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
                contributions = emptyList(),
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
                contributions = emptyList(),
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

        @Test
        fun `network is Verified with Amount is NotFound and QuoteStatus is Data`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = fullQuote.toStatus().some(),
                contributions = emptyList(),
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
                contributions = emptyList(),
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
                contributions = emptyList(),
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null).toStatus()
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Loading {

        @Test
        fun `network is null`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = none(),
                maybeQuoteStatus = none(), // not relevant for this test,
                contributions = emptyList(),
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
                contributions = emptyList(),
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
                contributions = emptyList(),
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
        fun `GIVEN network is Verified and no quote WHEN create THEN Custom carries no extra balance`() {
            // Arrange
            val maybeQuoteStatus = none<QuoteStatus>()

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                contributions = emptyList(),
            )

            // Assert
            val expected = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Custom(
                    amount = BigDecimal.TEN,
                    fiatAmount = null,
                    fiatRate = null,
                    priceChange = null,
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
        fun `GIVEN network is Verified and no contributions WHEN create THEN NoQuote has no extra balance`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                contributions = emptyList(),
            )

            // Assert
            val expected = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.TEN,
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
        fun `GIVEN network is Verified and no contributions WHEN create THEN Loaded has no extra balance`() {
            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = maybeQuoteStatus,
                contributions = emptyList(),
            )

            // Assert
            val expected = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN * fullQuote.fiatRate,
                fiatRate = fullQuote.fiatRate,
                priceChange = fullQuote.priceChange,
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
     * The extra balances arrive already resolved from the seam's `BalanceContributionProvider`s, so the factory
     * neither matches nor stamps anything — it stores the list and reports one source per contribution.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContributionsFromSeam {

        @Test
        fun `GIVEN staking contribution WHEN create THEN it is stored as is`() {
            // Arrange
            val currency = cryptoCurrencyFactory.cardano
            val contribution = stakeKitBalance(currency).copy(isStakedIncludedInNetworkBalance = true)

            // Act
            val actual = create(
                currency = currency,
                contributions = listOf(contribution),
            )

            // Assert — the contribution is the only source; readers reach it through `stakingBalanceData`
            Truth.assertThat(actual.value.contributions).containsExactly(contribution)
        }

        @Test
        fun `GIVEN a contribution that is not staking WHEN create THEN it is stored as is`() {
            // Arrange — yield supply contributes zero, but it still has to reach the status
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
                contributions = listOf(contribution),
            )

            // Assert
            Truth.assertThat(actual.value.contributions).containsExactly(contribution)
        }

        @Test
        fun `GIVEN cached contribution WHEN create THEN its source degrades the total source`() {
            // Arrange
            val currency = cryptoCurrencyFactory.cardano
            val contribution = stakeKitBalance(currency).copy(source = StatusSource.ONLY_CACHE)

            // Act
            val actual = create(
                currency = currency,
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
                contributions = listOf(fresh, stale),
            )

            // Assert — one source per contribution, in order, and the worst of them wins the total
            Truth.assertThat(actual.value.sources.contributionSources)
                .containsExactly(StatusSource.ACTUAL, StatusSource.ONLY_CACHE)
                .inOrder()
            Truth.assertThat(actual.value.sources.total).isEqualTo(StatusSource.ONLY_CACHE)
        }

        @Test
        fun `GIVEN no contributions WHEN create THEN nothing is attached`() {
            // Act
            val actual = create(currency = cryptoCurrencyFactory.cardano, contributions = emptyList())

            // Assert
            Truth.assertThat(actual.value.contributions).isEmpty()
            Truth.assertThat(actual.value.sources.contributionSources).isEmpty()
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldSupplyStatusSource {

        private val networkYieldStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("4"),
        )
        private val positionYieldStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = false,
            effectiveProtocolBalance = BigDecimal("7"),
        )
        private val token = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum)
        private val customToken = token.copy(isCustom = true)

        @ParameterizedTest
        @MethodSource("provideYieldSourceModels")
        fun `GIVEN yield status in network and contribution WHEN create THEN yield comes from the toggled source`(
            model: YieldSourceModel,
        ) {
            // Arrange
            val networkStatus = NetworkStatus(
                network = model.currency.network,
                value = createVerified(
                    amounts = mapOf(model.currency.id to Amount.Loaded(value = BigDecimal.TEN)),
                    yieldSupplyStatuses = mapOf(model.currency.id to networkYieldStatus),
                ),
            )

            // Act
            val actual = CryptoCurrencyStatusFactory.create(
                currency = model.currency,
                maybeNetworkStatus = networkStatus.some(),
                maybeQuoteStatus = QuoteStatus(
                    rawCurrencyId = model.currency.id.rawCurrencyId!!,
                    value = model.quote,
                ).some(),
                contributions = model.contributions,
                isYieldContributionEnabled = model.isYieldPositionsSourceEnabled,
            )

            // Assert
            Truth.assertThat(actual.value).isInstanceOf(model.expectedShape)
            Truth.assertThat(actual.value.yieldSupplyStatus).isEqualTo(model.expectedField)
            Truth.assertThat(actual.value.yieldSupply).isEqualTo(model.expectedYieldSupply)
        }

        private fun provideYieldSourceModels(): List<YieldSourceModel> {
            val contribution = YieldSupplyContribution(status = positionYieldStatus, source = StatusSource.ACTUAL)

            return listOf(
                YieldSourceModel(
                    description = "disabled, Loaded: the field carries the network status and wins over a contribution",
                    currency = token,
                    quote = fullQuote,
                    contributions = listOf(contribution),
                    isYieldPositionsSourceEnabled = false,
                    expectedShape = CryptoCurrencyStatus.Loaded::class.java,
                    expectedField = networkYieldStatus,
                    expectedYieldSupply = networkYieldStatus,
                ),
                YieldSourceModel(
                    description = "enabled, Loaded: empty field, yield from the contribution",
                    currency = token,
                    quote = fullQuote,
                    contributions = listOf(contribution),
                    isYieldPositionsSourceEnabled = true,
                    expectedShape = CryptoCurrencyStatus.Loaded::class.java,
                    expectedField = null,
                    expectedYieldSupply = positionYieldStatus,
                ),
                YieldSourceModel(
                    description = "enabled, Loaded, no contribution: no fallback to the network status",
                    currency = token,
                    quote = fullQuote,
                    contributions = emptyList(),
                    isYieldPositionsSourceEnabled = true,
                    expectedShape = CryptoCurrencyStatus.Loaded::class.java,
                    expectedField = null,
                    expectedYieldSupply = null,
                ),
                YieldSourceModel(
                    description = "enabled, NoQuote: empty field, yield from the contribution",
                    currency = token,
                    quote = QuoteStatus.Empty,
                    contributions = listOf(contribution),
                    isYieldPositionsSourceEnabled = true,
                    expectedShape = CryptoCurrencyStatus.NoQuote::class.java,
                    expectedField = null,
                    expectedYieldSupply = positionYieldStatus,
                ),
                YieldSourceModel(
                    description = "disabled, NoQuote: network status",
                    currency = token,
                    quote = QuoteStatus.Empty,
                    contributions = listOf(contribution),
                    isYieldPositionsSourceEnabled = false,
                    expectedShape = CryptoCurrencyStatus.NoQuote::class.java,
                    expectedField = networkYieldStatus,
                    expectedYieldSupply = networkYieldStatus,
                ),
                YieldSourceModel(
                    description = "enabled, Custom: empty field, yield from the contribution",
                    currency = customToken,
                    quote = fullQuote,
                    contributions = listOf(contribution),
                    isYieldPositionsSourceEnabled = true,
                    expectedShape = CryptoCurrencyStatus.Custom::class.java,
                    expectedField = null,
                    expectedYieldSupply = positionYieldStatus,
                ),
                YieldSourceModel(
                    description = "disabled, Custom: network status",
                    currency = customToken,
                    quote = fullQuote,
                    contributions = listOf(contribution),
                    isYieldPositionsSourceEnabled = false,
                    expectedShape = CryptoCurrencyStatus.Custom::class.java,
                    expectedField = networkYieldStatus,
                    expectedYieldSupply = networkYieldStatus,
                ),
            )
        }
    }

    data class YieldSourceModel(
        val description: String,
        val currency: CryptoCurrency,
        val quote: QuoteStatus.Value,
        val contributions: List<BalanceContribution>,
        val isYieldPositionsSourceEnabled: Boolean,
        val expectedShape: Class<out CryptoCurrencyStatus.Value>,
        val expectedField: YieldSupplyStatus?,
        val expectedYieldSupply: YieldSupplyStatus?,
    ) {

        override fun toString(): String = description
    }

    /**
     * The spendable part of a contribution is folded into `amount` (and so into `fiatAmount`), so every reader of
     * `amount` keeps seeing what can be sent; a non-spendable balance such as a stake never is.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SpendableFold {

        private val token = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum)

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN contributions WHEN create THEN only their spendable part is folded into the amount`(
            model: FoldModel,
        ) {
            // Arrange
            val quote = QuoteStatus(rawCurrencyId = model.currency.id.rawCurrencyId!!, value = model.quote)

            // Act
            val actual = create(
                currency = model.currency,
                contributions = model.contributions,
                maybeQuoteStatus = quote.some(),
            )

            // Assert
            Truth.assertThat(actual.value).isInstanceOf(model.expectedShape)
            Truth.assertThat(FoldResult(actual.value.amount, actual.value.fiatAmount)).isEqualTo(model.expected)
            Truth.assertThat(actual.value.contributions).isEqualTo(model.contributions)
        }

        private fun provideTestModels(): List<FoldModel> {
            val stake = FakeContribution(total = BigDecimal("5"), spendable = BigDecimal.ZERO)
            val supplied = FakeContribution(total = BigDecimal.ZERO, spendable = BigDecimal("7"))
            val custom = token.copy(isCustom = true)

            return listOf(
                FoldModel(
                    description = "Loaded, no contributions: the network amount",
                    currency = token,
                    quote = fullQuote,
                    contributions = emptyList(),
                    expectedShape = CryptoCurrencyStatus.Loaded::class.java,
                    expected = FoldResult(amount = BigDecimal("10"), fiatAmount = BigDecimal("18000.0")),
                ),
                FoldModel(
                    description = "Loaded, stake only: nothing folded",
                    currency = token,
                    quote = fullQuote,
                    contributions = listOf(stake),
                    expectedShape = CryptoCurrencyStatus.Loaded::class.java,
                    expected = FoldResult(amount = BigDecimal("10"), fiatAmount = BigDecimal("18000.0")),
                ),
                FoldModel(
                    description = "Loaded, spendable contribution next to a stake: only the spendable part",
                    currency = token,
                    quote = fullQuote,
                    contributions = listOf(supplied, stake),
                    expectedShape = CryptoCurrencyStatus.Loaded::class.java,
                    expected = FoldResult(amount = BigDecimal("17"), fiatAmount = BigDecimal("30600.0")),
                ),
                FoldModel(
                    description = "NoQuote, spendable contribution: folded, no fiat",
                    currency = token,
                    quote = QuoteStatus.Empty,
                    contributions = listOf(supplied),
                    expectedShape = CryptoCurrencyStatus.NoQuote::class.java,
                    expected = FoldResult(amount = BigDecimal("17"), fiatAmount = null),
                ),
                FoldModel(
                    description = "Custom, spendable contribution: folded",
                    currency = custom,
                    quote = fullQuote,
                    contributions = listOf(supplied),
                    expectedShape = CryptoCurrencyStatus.Custom::class.java,
                    expected = FoldResult(amount = BigDecimal("17"), fiatAmount = BigDecimal("30600.0")),
                ),
            )
        }
    }

    data class FoldModel(
        val description: String,
        val currency: CryptoCurrency,
        val quote: QuoteStatus.Value,
        val contributions: List<BalanceContribution>,
        val expectedShape: Class<out CryptoCurrencyStatus.Value>,
        val expected: FoldResult,
    ) {

        override fun toString(): String = description
    }

    data class FoldResult(val amount: BigDecimal?, val fiatAmount: BigDecimal?)

    private data class FakeContribution(val total: BigDecimal, val spendable: BigDecimal) : BalanceContribution {
        override val kind: String = "fake"
        override val source: StatusSource = StatusSource.ACTUAL
        override fun totalDeltaCryptoAmount(): BigDecimal = total
        override fun spendableDeltaCryptoAmount(): BigDecimal = spendable
    }

    // region Extra-balance fixtures
    private fun create(
        currency: CryptoCurrency,
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
            contributions = contributions,
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