package com.tangem.common.ui.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.BalanceItem
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale
import kotlin.reflect.KClass

/**
 * Characterizes the earn badge [TokenItemStateConverter] puts into a token title: which rate the badge shows, which
 * wording it uses and whether it counts as active. The cases pin what the converter does today, quirks included.
 *
 * Staked balances are mocked: the converter only reads `validatorAddress` off the items, never the totals.
 */
internal class TokenItemStateConverterEarnBadgeTest {

    private val factory = MockCryptoCurrencyFactory()
    private val ethereum = factory.createCoin(Blockchain.Ethereum)
    private val solana = factory.createCoin(Blockchain.Solana)
    private val cardano = factory.createCoin(Blockchain.Cardano)
    private val stakeToken = factory.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = "0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6",
    )
    private val yieldToken = factory.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    )

    private val yieldApy = BigDecimal("5.851295672613467")
    private val yieldApyMap = mapOf(yieldToken.yieldSupplyKey() to yieldApy)
    private val yieldBadge = badge(R.string.yield_module_earn_badge, yieldApy)

    private val networkAddress = NetworkAddress.Single(
        NetworkAddress.Address("address", NetworkAddress.Address.Type.Primary),
    )

    private var originalLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StakingAlone {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN staking option and no stake WHEN convert THEN badge shows the rate and is inactive`(
            model: StakingBadgeModel,
        ) {
            // Arrange
            val converter = converter(stakingApyMap = mapOf(model.currency to model.availability))

            // Act
            val actual = converter.contentTitle(loaded(model.currency))

            // Assert
            assertThat(actual).isEqualTo(expectedTitle(model.currency, earnApy = model.expectedBadge))
        }

        @Test
        fun `GIVEN staking rate below the displayed precision WHEN convert THEN badge shows the rounded number`() {
            // Arrange
            val availability = StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.00001")),
            )
            val converter = converter(stakingApyMap = mapOf(solana to availability))

            // Act
            val actual = converter.contentTitle(loaded(solana))

            // Assert
            val expected = expectedTitle(solana, earnApy = badge(R.string.yield_module_earn_badge, "0.00"))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN coin AND yield map of another token WHEN convert THEN yield map is ignored`() {
            // Arrange
            val availability = StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.07", type = RewardType.APR)),
            )
            val converter = converter(yieldModuleApyMap = yieldApyMap, stakingApyMap = mapOf(solana to availability))

            // Act
            val actual = converter.contentTitle(loaded(solana))

            // Assert
            val expected = expectedTitle(solana, earnApy = badge(R.string.staking_apr_earn_badge, "7.00"))
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            StakingBadgeModel(
                description = "P2P ETH pool",
                currency = ethereum,
                availability = StakingAvailability.Available(p2pOption(apy = "0.04")),
                expectedBadge = badge(R.string.yield_module_earn_badge, "4.00"),
            ),
            StakingBadgeModel(
                description = "StakeKit coin with APR",
                currency = solana,
                availability = StakingAvailability.Available(
                    stakeKitOption(validator("v1", preferred = true, rate = "0.07", type = RewardType.APR)),
                ),
                expectedBadge = badge(R.string.staking_apr_earn_badge, "7.00"),
            ),
            StakingBadgeModel(
                description = "StakeKit coin with APY",
                currency = cardano,
                availability = StakingAvailability.Available(
                    stakeKitOption(
                        validator("v1", preferred = true, rate = "0.0312", type = RewardType.APY),
                        integrationId = StakingIntegrationID.StakeKit.Coin.Cardano,
                    ),
                ),
                expectedBadge = badge(R.string.yield_module_earn_badge, "3.12"),
            ),
            StakingBadgeModel(
                description = "StakeKit token",
                currency = stakeToken,
                availability = StakingAvailability.Available(
                    stakeKitOption(
                        validator("v1", preferred = true, rate = "0.055", type = RewardType.APY),
                        integrationId = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
                    ),
                ),
                expectedBadge = badge(R.string.yield_module_earn_badge, "5.50"),
            ),
            StakingBadgeModel(
                description = "StakeKit coin with unknown reward type",
                currency = solana,
                availability = StakingAvailability.Available(
                    stakeKitOption(validator("v1", preferred = true, rate = "0.06", type = RewardType.UNKNOWN)),
                ),
                expectedBadge = badge(R.string.yield_module_earn_badge, "6.00"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FullAvailability {

        @Test
        fun `GIVEN Full availability AND no stake WHEN convert THEN no badge`() {
            // Arrange
            val availability = StakingAvailability.Full(
                stakeKitOption(validator("v1", preferred = true, rate = "0.07")),
            )
            val converter = converter(stakingApyMap = mapOf(solana to availability))

            // Act
            val actual = converter.contentTitle(loaded(solana))

            // Assert
            assertThat(actual).isEqualTo(expectedTitle(solana))
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Full availability AND a stake WHEN convert THEN badge is shown and active`(
            model: StakedBadgeModel,
        ) {
            // Arrange
            val converter = converter(stakingApyMap = mapOf(model.currency to model.availability))

            // Act
            val actual = converter.contentTitle(loaded(model.currency, stakingBalance = model.stake))

            // Assert
            val expected = expectedTitle(model.currency, earnApy = model.expectedBadge, isEarnApyActive = true)
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            StakedBadgeModel(
                description = "StakeKit balance",
                currency = solana,
                availability = StakingAvailability.Full(
                    stakeKitOption(validator("v1", preferred = true, rate = "0.07", type = RewardType.APR)),
                ),
                stake = stakeKitBalance("v1"),
                expectedBadge = badge(R.string.staking_apr_earn_badge, "7.00"),
            ),
            StakedBadgeModel(
                description = "P2P contribution",
                currency = ethereum,
                availability = StakingAvailability.Full(p2pOption(apy = "0.04")),
                stake = mockk<StakingBalance.Data.P2PEthPool>(),
                expectedBadge = badge(R.string.yield_module_earn_badge, "4.00"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldModeAlone {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN token in yield map WHEN convert THEN badge shows the raw percent and follows yield activity`(
            model: YieldStatusModel,
        ) {
            // Arrange
            val converter = converter(yieldModuleApyMap = yieldApyMap)

            // Act
            val actual = converter.contentTitle(loaded(yieldToken, yieldSupplyStatus = model.yieldSupplyStatus))

            // Assert
            val expected = expectedTitle(yieldToken, earnApy = yieldBadge, isEarnApyActive = model.expectedActive)
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            YieldStatusModel(description = "active", yieldSupplyStatus = yieldSupplyStatus(true), expectedActive = true),
            YieldStatusModel(
                description = "inactive",
                yieldSupplyStatus = yieldSupplyStatus(false),
                expectedActive = false,
            ),
            YieldStatusModel(description = "no status", yieldSupplyStatus = null, expectedActive = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldModeAndStaking {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN token in yield map AND staking map WHEN convert THEN yield badge wins with yield activity`(
            model: ActivityModel,
        ) {
            // Arrange
            val availability = StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.07", type = RewardType.APR)),
            )
            val converter = converter(yieldModuleApyMap = yieldApyMap, stakingApyMap = mapOf(yieldToken to availability))
            val status = loaded(
                currency = yieldToken,
                stakingBalance = if (model.isStakingActive) stakeKitBalance("v1") else null,
                yieldSupplyStatus = yieldSupplyStatus(model.isYieldActive),
            )

            // Act
            val actual = converter.contentTitle(status)

            // Assert
            val expected = expectedTitle(yieldToken, earnApy = yieldBadge, isEarnApyActive = model.isYieldActive)
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            ActivityModel(isYieldActive = true, isStakingActive = true),
            ActivityModel(isYieldActive = true, isStakingActive = false),
            ActivityModel(isYieldActive = false, isStakingActive = true),
            ActivityModel(isYieldActive = false, isStakingActive = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NonContentStatuses {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN yield and staking maps AND status without balance WHEN convert THEN title has no badge`(
            model: NonContentStatusModel,
        ) {
            // Arrange
            val availability = StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.07", type = RewardType.APR)),
            )
            val converter = converter(yieldModuleApyMap = yieldApyMap, stakingApyMap = mapOf(yieldToken to availability))
            val status = CryptoCurrencyStatus(currency = yieldToken, value = model.value)

            // Act
            val actual = converter.convert(status)

            // Assert
            assertThat(actual).isInstanceOf(model.expectedState.java)
            assertThat(actual.titleState).isEqualTo(expectedTitle(yieldToken))
        }

        private fun provideTestModels() = listOf(
            NonContentStatusModel(
                description = "Loading",
                value = CryptoCurrencyStatus.Loading,
                expectedState = TokenItemState.Loading::class,
            ),
            NonContentStatusModel(
                description = "Unreachable",
                value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
                expectedState = TokenItemState.Unreachable::class,
            ),
        )
    }

    private fun converter(
        yieldModuleApyMap: Map<String, BigDecimal> = emptyMap(),
        stakingApyMap: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
    ) = TokenItemStateConverter(
        appCurrency = AppCurrency.Default,
        yieldModuleApyMap = yieldModuleApyMap,
        stakingApyMap = stakingApyMap,
        iconStateProvider = { CurrencyIconState.Loading },
        subtitleStateProvider = { TokenItemState.SubtitleState.Loading },
        subtitle2StateProvider = { TokenItemState.Subtitle2State.Loading },
        fiatAmountStateProvider = { TokenItemState.FiatAmountState.Loading },
        promoBannerProvider = { TokenItemState.PromoBannerState.Empty },
    )

    private fun TokenItemStateConverter.contentTitle(status: CryptoCurrencyStatus): TokenItemState.TitleState {
        return (convert(status) as TokenItemState.Content).titleState as TokenItemState.TitleState.Content
    }

    private fun expectedTitle(
        currency: CryptoCurrency,
        earnApy: TextReference? = null,
        isEarnApyActive: Boolean = false,
    ) = TokenItemState.TitleState.Content(
        text = stringReference(currency.name),
        earnApy = earnApy,
        isEarnApyActive = isEarnApyActive,
    )

    private fun badge(res: Int, rate: Any) = resourceReference(res, wrappedList(rate))

    private fun loaded(
        currency: CryptoCurrency,
        stakingBalance: StakingBalance.Data? = null,
        yieldSupplyStatus: YieldSupplyStatus? = null,
    ) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = yieldSupplyStatus,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
            contributions = listOfNotNull(stakingBalance),
        ),
    )

    private fun yieldSupplyStatus(isActive: Boolean) = YieldSupplyStatus(
        isActive = isActive,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = null,
    )

    /**
     * A real option over a mocked [Yield]: stubbing `integrationId` on an option mock would make MockK retransform
     * the enum implementations of `StakingIntegrationID`, which the JVM rejects.
     */
    private fun stakeKitOption(
        vararg validatorList: Yield.Validator,
        integrationId: StakingIntegrationID.StakeKit = StakingIntegrationID.StakeKit.Coin.Solana,
    ): StakingOption.StakeKit {
        val yieldModel: Yield = mockk {
            every { validators } returns validatorList.toList()
            every { apy } returns BigDecimal("0.10")
            every { token } returns mockk()
            every { isAvailable } returns true
        }
        return StakingOption.StakeKit(integrationId = integrationId, yield = yieldModel)
    }

    private fun p2pOption(apy: String): StakingOption.P2PEthPool = mockk {
        every { this@mockk.apy } returns BigDecimal(apy)
    }

    private fun validator(
        address: String,
        preferred: Boolean,
        rate: String,
        type: RewardType = RewardType.APY,
    ): Yield.Validator = Yield.Validator(
        address = address,
        status = Yield.Validator.ValidatorStatus.ACTIVE,
        name = "validator-$address",
        isPreferred = preferred,
        isStrategicPartner = false,
        rewardInfo = RewardInfo(rate = BigDecimal(rate), type = type),
    )

    private fun stakeKitBalance(vararg validatorAddresses: String?): StakingBalance.Data.StakeKit = mockk {
        every { balance } returns mockk {
            every { items } returns validatorAddresses.map { address ->
                mockk<BalanceItem> { every { validatorAddress } returns address }
            }
        }
    }

    internal data class StakingBadgeModel(
        val description: String,
        val currency: CryptoCurrency,
        val availability: StakingAvailability,
        val expectedBadge: TextReference,
    ) {
        override fun toString(): String = description
    }

    internal data class StakedBadgeModel(
        val description: String,
        val currency: CryptoCurrency,
        val availability: StakingAvailability,
        val stake: StakingBalance.Data,
        val expectedBadge: TextReference,
    ) {
        override fun toString(): String = description
    }

    internal data class YieldStatusModel(
        val description: String,
        val yieldSupplyStatus: YieldSupplyStatus?,
        val expectedActive: Boolean,
    ) {
        override fun toString(): String = description
    }

    internal data class ActivityModel(val isYieldActive: Boolean, val isStakingActive: Boolean)

    internal data class NonContentStatusModel(
        val description: String,
        val value: CryptoCurrencyStatus.Value,
        val expectedState: KClass<out TokenItemState>,
    ) {
        override fun toString(): String = description
    }
}