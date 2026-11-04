package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.BalanceItem
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
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

/**
 * Tests the earn-badge APY resolution: which rate a token's badge shows and whether it counts as already earning.
 *
 * The converter reads the stake through `stakingBalanceData`, so a token whose only stake arrives as a balance
 * contribution must still be reported as earning. And the StakeKit rate selection has real branching: an active
 * stake answers with its own validator's rate, and only falls back to the best preferred validator when the stake
 * doesn't point at a known one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnApyConverterTest {

    private val currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
    private val yieldToken = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Monad,
        contractAddress = "0x754704bc059f8c67012fed69bc8a327a5aafb603",
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

    @Test
    fun `GIVEN a yield apy in percent with a long scale WHEN convert THEN badge shows two decimals of the fraction`() {
        // Arrange
        val converter = EarnApyConverter(
            yieldModuleApyMap = mapOf(yieldToken.yieldSupplyKey() to BigDecimal("5.851295672613467")),
            stakingApyMap = emptyMap(),
        )

        // Act
        val actual = converter.convert(status(currency = yieldToken))

        // Assert
        val expected = EarnApyConverter.EarnApyInfo(
            text = resourceReference(R.string.yield_module_earn_badge, wrappedList("5.85")),
            isActive = false,
            rate = BigDecimal("0.05851295672613467"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN Full availability AND the stake arrives as a contribution WHEN convert THEN badge is active`() {
        // Arrange — Full hides the badge for tokens without a stake
        val converter = converter(
            StakingAvailability.Full(stakeKitOption(validator("v1", preferred = true, rate = "0.07"))),
        )
        val status = status(stakingBalance = stakeKitBalance("v1"))

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.07", isActive = true))
    }

    @Test
    fun `GIVEN an active stake matching a validator WHEN convert THEN that validator's rate wins`() {
        // Arrange — the stake sits on the non-preferred v2, whose rate must beat the better preferred one
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.09"),
                    validator("v2", preferred = false, rate = "0.05"),
                ),
            ),
        )
        val status = status(stakingBalance = stakeKitBalance("v2"))

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.05", isActive = true))
    }

    @Test
    fun `GIVEN an active stake with no matching validator WHEN convert THEN best preferred rate is used`() {
        // Arrange — the stake points at an unknown validator, so the fallback must pick the best
        // preferred rate and ignore the higher non-preferred one
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.03"),
                    validator("v2", preferred = true, rate = "0.08"),
                    validator("v3", preferred = false, rate = "0.99"),
                ),
            ),
        )
        val status = status(stakingBalance = stakeKitBalance("unknown"))

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.08", isActive = true))
    }

    @Test
    fun `GIVEN no stake WHEN convert THEN best preferred rate is used and badge is not active`() {
        // Arrange
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.03"),
                    validator("v2", preferred = true, rate = "0.08"),
                    validator("v3", preferred = false, rate = "0.99"),
                ),
            ),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.08", isActive = false))
    }

    @Test
    fun `GIVEN a P2P option WHEN convert THEN its own APY is used`() {
        // Arrange
        val converter = converter(StakingAvailability.Available(p2pOption(apy = BigDecimal("0.04"))))

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.04", isActive = false))
    }

    @Test
    fun `GIVEN an APR validator WHEN convert THEN the APR badge text is used`() {
        // Arrange
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.06", type = RewardType.APR)),
            ),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        val expected = earnApyInfo(rate = "0.06", isActive = false, badgeRes = R.string.staking_apr_earn_badge)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN Full availability AND no stake WHEN convert THEN no badge`() {
        // Arrange — no free capacity and nothing staked: the badge must not advertise an unavailable option
        val converter = converter(
            StakingAvailability.Full(stakeKitOption(validator("v1", preferred = true, rate = "0.07"))),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isNull()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldModeAndStaking {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN yield mode and staking for one token WHEN convert THEN yield mode result wins`(
            model: YieldAndStakingModel,
        ) {
            // Arrange
            val converter = EarnApyConverter(
                yieldModuleApyMap = mapOf(yieldToken.yieldSupplyKey() to BigDecimal("5.5")),
                stakingApyMap = mapOf(
                    yieldToken to StakingAvailability.Available(
                        stakeKitOption(validator("v1", preferred = true, rate = "0.07")),
                    ),
                ),
            )
            val status = status(
                stakingBalance = if (model.isStakingActive) stakeKitBalance("v1") else null,
                currency = yieldToken,
                yieldSupplyStatus = YieldSupplyStatus(model.isYieldActive, true, true, null),
            )

            // Act
            val actual = converter.convert(status)

            // Assert
            assertThat(actual).isEqualTo(yieldModeApyInfo(isActive = model.isYieldActive))
        }

        private fun provideTestModels() = listOf(
            YieldAndStakingModel(isYieldActive = true, isStakingActive = true),
            YieldAndStakingModel(isYieldActive = true, isStakingActive = false),
            YieldAndStakingModel(isYieldActive = false, isStakingActive = true),
            YieldAndStakingModel(isYieldActive = false, isStakingActive = false),
        )
    }

    @Test
    fun `GIVEN a coin AND a non-empty yield map WHEN convert THEN yield mode is skipped and staking is used`() {
        // Arrange
        val converter = converter(
            availability = StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.07")),
            ),
            yieldModuleApyMap = mapOf(yieldToken.yieldSupplyKey() to BigDecimal("5.5")),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isEqualTo(stakingApyInfo(isActive = false))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldKeyMatching {

        private val usdcEvm = MockCryptoCurrencyFactory().createToken(
            blockchain = Blockchain.Ethereum,
            id = "usd-coin",
            contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
        )
        private val usdcSolana = MockCryptoCurrencyFactory().createToken(
            blockchain = Blockchain.Solana,
            id = "usd-coin",
            contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a yield key in some letter case WHEN convert THEN the match follows the network's address case rule`(
            model: YieldKeyModel,
        ) {
            // Arrange
            val converter = EarnApyConverter(
                yieldModuleApyMap = mapOf(model.key to BigDecimal("5.5")),
                stakingApyMap = emptyMap(),
            )

            // Act
            val actual = converter.convert(status(currency = model.token))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        // Pins current behaviour: yieldSupplyKey lowercases the contract, so a Solana key in original case never matches
        private fun provideTestModels() = listOf(
            YieldKeyModel(
                description = "EVM token AND upper-case key -> matched",
                token = usdcEvm,
                key = "ETHEREUM_0XA0B86991C6218B36C1D19D4A2E9EB0CE3606EB48",
                expected = yieldModeApyInfo(isActive = false),
            ),
            YieldKeyModel(
                description = "Solana token AND original-case key -> not matched",
                token = usdcSolana,
                key = "solana_EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                expected = null,
            ),
            YieldKeyModel(
                description = "Solana token AND lower-case key -> matched",
                token = usdcSolana,
                key = "solana_epjfwdd5aufqssqem2qn1xzybapc8g4weggkzwytdt1v",
                expected = yieldModeApyInfo(isActive = false),
            ),
        )
    }

    @Test
    fun `GIVEN a staking rate below one hundredth of a percent WHEN convert THEN badge shows the lower-than text`() {
        // Arrange
        val converter = converter(
            StakingAvailability.Available(stakeKitOption(validator("v1", preferred = true, rate = "0.00001"))),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        val expected = EarnApyConverter.EarnApyInfo(
            text = resourceReference(R.string.yield_module_earn_badge, wrappedList("<0.01")),
            isActive = false,
            rate = BigDecimal("0.00001"),
            isStaking = true,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a token keyed StakeKit option WHEN convert THEN staking badge is shown for the token`() {
        // Arrange
        val converter = EarnApyConverter(
            yieldModuleApyMap = emptyMap(),
            stakingApyMap = mapOf(
                yieldToken to StakingAvailability.Available(
                    stakeKitOption(validator("v1", preferred = true, rate = "0.07")),
                ),
            ),
        )

        // Act
        val actual = converter.convert(status(currency = yieldToken))

        // Assert
        assertThat(actual).isEqualTo(stakingApyInfo(isActive = false))
    }

    // region Fixtures

    private fun converter(
        availability: StakingAvailability,
        yieldModuleApyMap: Map<String, BigDecimal> = emptyMap(),
    ) = EarnApyConverter(
        yieldModuleApyMap = yieldModuleApyMap,
        stakingApyMap = mapOf(currency to availability),
    )

    private fun status(
        stakingBalance: StakingBalance.Data? = null,
        currency: CryptoCurrency = this.currency,
        yieldSupplyStatus: YieldSupplyStatus? = null,
    ): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = yieldSupplyStatus,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = mockk(relaxed = true),
            sources = CryptoCurrencyStatus.Sources(),
            contributions = listOfNotNull(stakingBalance),
        ),
    )

    /**
     * A real option over a mocked [Yield]: stubbing `integrationId` on an option mock would make MockK retransform
     * `StakingIntegrationID`'s enum implementations, which the JVM rejects ("cannot change the class modifiers").
     */
    private fun stakeKitOption(vararg validatorList: Yield.Validator): StakingOption.StakeKit {
        val yieldModel: Yield = mockk {
            every { validators } returns validatorList.toList()
            every { apy } returns BigDecimal("0.10")
            every { token } returns mockk()
            every { isAvailable } returns true
        }
        return StakingOption.StakeKit(
            integrationId = StakingIntegrationID.StakeKit.Coin.Solana,
            yield = yieldModel,
        )
    }

    private fun p2pOption(apy: BigDecimal): StakingOption.P2PEthPool = mockk {
        every { this@mockk.apy } returns apy
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

    /**
     * A mocked stake is safe here, unlike in the totals tests: the converter reads only `validatorAddress` off the
     * items — no member math for MockK to intercept.
     */
    private fun stakeKitBalance(vararg validatorAddresses: String?): StakingBalance.Data.StakeKit = mockk {
        every { balance } returns mockk {
            every { items } returns validatorAddresses.map { address ->
                mockk<BalanceItem> { every { validatorAddress } returns address }
            }
        }
    }

    /** The converter renders the rate itself; the expected string is produced by the same formatter. */
    private fun earnApyInfo(
        rate: String,
        isActive: Boolean,
        badgeRes: Int = R.string.yield_module_earn_badge,
    ): EarnApyConverter.EarnApyInfo {
        val rateValue = BigDecimal(rate)
        val rateText = rateValue.format { percent(canBeLower = true, withPercentSign = false) }

        return EarnApyConverter.EarnApyInfo(
            text = resourceReference(badgeRes, wrappedList(rateText)),
            isActive = isActive,
            rate = rateValue,
            isStaking = true,
        )
    }

    /** The Yield Mode badge for a 5.5% APY: the converter shifts the percent into a fraction (`0.055`). */
    private fun yieldModeApyInfo(isActive: Boolean) = EarnApyConverter.EarnApyInfo(
        text = resourceReference(R.string.yield_module_earn_badge, wrappedList("5.50")),
        isActive = isActive,
        rate = BigDecimal("0.055"),
        isStaking = false,
    )

    /** The StakeKit badge for a 7% APY, with the text as a literal instead of the production formatter's output. */
    private fun stakingApyInfo(isActive: Boolean) = EarnApyConverter.EarnApyInfo(
        text = resourceReference(R.string.yield_module_earn_badge, wrappedList("7.00")),
        isActive = isActive,
        rate = BigDecimal("0.07"),
        isStaking = true,
    )

    data class YieldAndStakingModel(val isYieldActive: Boolean, val isStakingActive: Boolean)

    data class YieldKeyModel(
        val description: String,
        val token: CryptoCurrency.Token,
        val key: String,
        val expected: EarnApyConverter.EarnApyInfo?,
    ) {
        override fun toString() = description
    }

    // endregion
}