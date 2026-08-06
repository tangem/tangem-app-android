package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

internal class ForYouEarnOpportunitiesConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Nested
    inner class StateSelection {

        @Test
        fun `GIVEN empty portfolio WHEN convert THEN no-tokens state`() {
            // Arrange
            val converter = createConverter()

            // Act
            val result = converter.convert(createSelectedPortfolio()) as EarnOpportunitiesUM.Content

            // Assert
            assertThat(result.subtitleRes).isEqualTo(R.string.for_you_earn_opportunities_no_available_tokens)
        }

        @Test
        fun `GIVEN currencies without any earn option WHEN convert THEN no-tokens state`() {
            // Arrange — no yield map entries and no staking availability → nothing is earn-eligible
            val status = createStatus(createEarnCurrency(), createEarnStatusValue())
            val converter = createConverter()

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_no_available_tokens)
        }

        @Test
        fun `GIVEN every earn-eligible token already staked WHEN convert THEN all-active state`() {
            // Arrange
            val currency = createEarnCurrency()
            val status = createStatus(currency, createEarnStatusValue(isStakingActive = true))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(currency to stakingAvailable(apy = BigDecimal("0.05"))),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_all_tokens_active)
        }

        @Test
        fun `GIVEN earn-eligible token not yet earning WHEN convert THEN potential-rewards state`() {
            // Arrange
            val currency = createEarnCurrency()
            val status = createStatus(currency, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(currency to stakingAvailable(apy = BigDecimal("0.05"))),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_tokens_rewards)
        }
    }

    @Nested
    inner class EarnEligibility {

        @Test
        fun `GIVEN zero balance and inactive earn WHEN convert THEN token is not eligible`() {
            // Arrange — nothing to earn on: no balance and not already earning
            val currency = createEarnCurrency()
            val status = createStatus(currency, createEarnStatusValue(fiatAmount = BigDecimal.ZERO))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(currency to stakingAvailable(apy = BigDecimal("0.05"))),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_no_available_tokens)
        }

        @Test
        fun `GIVEN zero balance but active stake WHEN convert THEN token stays visible as active`() {
            // Arrange
            val currency = createEarnCurrency()
            val status = createStatus(
                currency,
                createEarnStatusValue(fiatAmount = BigDecimal.ZERO, isStakingActive = true),
            )
            val converter = createConverter(
                yieldStakingAvailability = mapOf(currency to stakingAvailable(apy = BigDecimal("0.05"))),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_all_tokens_active)
        }

        @Test
        fun `GIVEN full staking pool without existing stake WHEN convert THEN token is not eligible`() {
            // Arrange — Full = no free capacity: new stakes are not offered
            val currency = createEarnCurrency()
            val status = createStatus(currency, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(
                    currency to StakingAvailability.Full(option = stakingOption(apy = BigDecimal("0.05"))),
                ),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_no_available_tokens)
        }

        @Test
        fun `GIVEN full staking pool with existing stake WHEN convert THEN token stays visible as active`() {
            // Arrange
            val currency = createEarnCurrency()
            val status = createStatus(
                currency,
                createEarnStatusValue(fiatAmount = BigDecimal("100"), isStakingActive = true),
            )
            val converter = createConverter(
                yieldStakingAvailability = mapOf(
                    currency to StakingAvailability.Full(option = stakingOption(apy = BigDecimal("0.05"))),
                ),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert
            assertThat((result as EarnOpportunitiesUM.Content).subtitleRes)
                .isEqualTo(R.string.for_you_earn_opportunities_all_tokens_active)
        }
    }

    @Nested
    inner class ApyResolution {

        @Test
        fun `GIVEN token eligible for both yield and staking WHEN convert THEN yield rate wins`() {
            // Arrange — yield 10.00% vs staking 50%: the reward must be computed from the yield rate
            val token = createEarnTokenCurrency()
            val status = createStatus(token, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            val converter = createConverter(
                yieldSupplyAvailability = mapOf(token.yieldSupplyKey() to BigDecimal("10.00")),
                yieldStakingAvailability = mapOf<CryptoCurrency, StakingAvailability>(
                    token to stakingAvailable(apy = BigDecimal("0.50")),
                ),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert — 100 * (10.00 / 100) = 10.00 per year, not 50
            assertThat((result as EarnOpportunitiesUM.Content).potentialReward)
                .isEqualTo(expectedPerYearReward(fiat = BigDecimal("100"), yieldPercent = BigDecimal("10.00")))
        }

        @Test
        fun `GIVEN active StakeKit stake WHEN convert THEN the staked validator's rate wins over best preferred`() {
            // Arrange — the user stakes with v2 (4%), while the best preferred validator offers 10%
            val staked = createEarnCurrency(tokenId = "ethereum", currencyId = "coin-staked")
            val stakedStatus = createStatus(
                staked,
                createEarnStatusValue(fiatAmount = BigDecimal("100"), stakingBalance = stakeKitBalance("v2")),
            )
            val fresh = createEarnCurrency(tokenId = "solana", currencyId = "coin-fresh")
            val freshStatus = createStatus(fresh, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(
                    staked to stakeKitAvailable(
                        validator(address = "v1", preferred = true, rate = BigDecimal("0.10")),
                        validator(address = "v2", preferred = false, rate = BigDecimal("0.04")),
                    ),
                    fresh to stakingAvailable(apy = BigDecimal("0.05")),
                ),
            )

            // Act
            val result = converter.convert(
                createSelectedPortfolio(createPortfolioStatus(listOf(stakedStatus, freshStatus))),
            )

            // Assert — the staked token's row shows the 4% of the validator actually staked with
            assertThat((result as EarnOpportunitiesUM.Content).rateOfRow("coin-staked"))
                .isEqualTo("APY " + BigDecimal("0.04").format { percent() })
        }

        @Test
        fun `GIVEN stake with unknown validator WHEN convert THEN falls back to best preferred validator rate`() {
            // Arrange — the staked validator is not among the option's validators
            val staked = createEarnCurrency(tokenId = "ethereum", currencyId = "coin-staked")
            val stakedStatus = createStatus(
                staked,
                createEarnStatusValue(fiatAmount = BigDecimal("100"), stakingBalance = stakeKitBalance("unknown")),
            )
            val fresh = createEarnCurrency(tokenId = "solana", currencyId = "coin-fresh")
            val freshStatus = createStatus(fresh, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(
                    staked to stakeKitAvailable(
                        validator(address = "v1", preferred = true, rate = BigDecimal("0.10")),
                        validator(address = "v2", preferred = true, rate = BigDecimal("0.12")),
                        validator(address = "v3", preferred = false, rate = BigDecimal("0.50")),
                    ),
                    fresh to stakingAvailable(apy = BigDecimal("0.05")),
                ),
            )

            // Act
            val result = converter.convert(
                createSelectedPortfolio(createPortfolioStatus(listOf(stakedStatus, freshStatus))),
            )

            // Assert — the best *preferred* rate (12%) is used; the non-preferred 50% is ignored
            assertThat((result as EarnOpportunitiesUM.Content).rateOfRow("coin-staked"))
                .isEqualTo("APY " + BigDecimal("0.12").format { percent() })
        }

        /** Extracts the rendered rate (the styled bottom-end text) of the row with the given [id]. */
        private fun EarnOpportunitiesUM.Content.rateOfRow(id: String): String {
            val row = items.first { it.tokenRowUM.id == id }.tokenRowUM as TangemTokenRowUM.Content
            val bottomEnd = row.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            return (bottomEnd.text as TextReference.StyledStr).value
        }

        @Test
        fun `GIVEN staking-only token WHEN convert THEN staking rate is used for the reward`() {
            // Arrange
            val currency = createEarnCurrency()
            val status = createStatus(currency, createEarnStatusValue(fiatAmount = BigDecimal("200")))
            val converter = createConverter(
                yieldStakingAvailability = mapOf(currency to stakingAvailable(apy = BigDecimal("0.04"))),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))

            // Assert — 200 * 0.04 = 8 per year
            val expectedTotal = BigDecimal("200").multiply(BigDecimal("0.04"))
            assertThat((result as EarnOpportunitiesUM.Content).potentialReward)
                .isEqualTo(expectedTotal.expectedPerYearText())
        }
    }

    @Nested
    inner class TypeResolution {

        @Test
        fun `GIVEN yield-eligible token row clicked WHEN convert THEN yield type carries the raw percent apy`() {
            // Arrange — the backend rate (10.00%) must reach the click callback unscaled
            val token = createEarnTokenCurrency()
            val status = createStatus(token, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            var clickedType: ForYouEarnOpportunitiesType? = null
            val converter = createConverter(
                yieldSupplyAvailability = mapOf(token.yieldSupplyKey() to BigDecimal("10.00")),
                onTokenClick = { _, _, type -> clickedType = type },
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))
            result.clickFirstRow()

            // Assert
            assertThat(clickedType).isEqualTo(ForYouEarnOpportunitiesType.YieldSupply(apy = "10.00"))
        }

        @Test
        fun `GIVEN staking-eligible token row clicked WHEN convert THEN staking type carries the integration id`() {
            // Arrange
            val currency = createEarnCurrency()
            val status = createStatus(currency, createEarnStatusValue(fiatAmount = BigDecimal("100")))
            val availability = stakingAvailable(apy = BigDecimal("0.05"))
            var clickedType: ForYouEarnOpportunitiesType? = null
            val converter = createConverter(
                yieldStakingAvailability = mapOf(currency to availability),
                onTokenClick = { _, _, type -> clickedType = type },
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(createPortfolioStatus(listOf(status))))
            result.clickFirstRow()

            // Assert — the id comes from the resolved staking option
            val option = (availability as StakingAvailability.Available).option
            assertThat(clickedType).isEqualTo(
                ForYouEarnOpportunitiesType.Staking(
                    integrationID = option.integrationId,
                    rewardType = EarnRewardType.APY,
                ),
            )
        }

        private fun EarnOpportunitiesUM.clickFirstRow() {
            val row = items.first().tokenRowUM as TangemTokenRowUM.Content
            row.onItemClick?.invoke()
        }
    }

    @Nested
    inner class AccountOrdering {

        @Test
        fun `GIVEN several accounts WHEN convert THEN accounts ordered by potential reward descending`() {
            // Arrange — same 5% rate; the second account holds more fiat (200 vs 100), so it earns more
            val smallHolding = createEarnCurrency(tokenId = "ethereum", currencyId = "coin-eth")
            val largeHolding = createEarnCurrency(tokenId = "solana", currencyId = "coin-sol")
            val smallAccount = createPortfolioStatus(
                currencies = listOf(createStatus(smallHolding, createEarnStatusValue(fiatAmount = BigDecimal("100")))),
                account = MockAccounts.createAccount(derivationIndex = 1),
            )
            val largeAccount = createPortfolioStatus(
                currencies = listOf(createStatus(largeHolding, createEarnStatusValue(fiatAmount = BigDecimal("200")))),
                account = MockAccounts.createAccount(derivationIndex = 2),
            )
            val converter = createConverter(
                yieldStakingAvailability = mapOf(
                    smallHolding to stakingAvailable(apy = BigDecimal("0.05")),
                    largeHolding to stakingAvailable(apy = BigDecimal("0.05")),
                ),
            )

            // Act
            val result = converter.convert(createSelectedPortfolio(smallAccount, largeAccount))

            // Assert — the higher-earning account's token leads the flat list
            assertThat((result as EarnOpportunitiesUM.Content).items.map { it.tokenRowUM.id })
                .containsExactly("coin-sol", "coin-eth")
                .inOrder()
        }
    }

    private fun createConverter(
        yieldSupplyAvailability: Map<String, BigDecimal> = emptyMap(),
        yieldStakingAvailability: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
        topEarnTokens: EarnTopToken? = null,
        isAccountsModeEnabled: Boolean = false,
        onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit = { _, _, _ -> },
    ) = ForYouEarnOpportunitiesConverter(
        appCurrency = appCurrency,
        isAccountsModeEnabled = isAccountsModeEnabled,
        expandClick = {},
        yieldSupplyAvailability = yieldSupplyAvailability,
        yieldStakingAvailability = yieldStakingAvailability,
        topEarnTokens = topEarnTokens,
        onTokenClick = onTokenClick,
        onAllEarnTokensClick = {},
        walletHeaders = emptyMap(),
    )

    // Real StakingIntegrationID values are used below: mocking the sealed interface makes mockk try to
    // retransform its enum implementations, which the JVM rejects ("cannot change the class modifiers").
    private fun stakingOption(apy: BigDecimal): StakingOption.P2PEthPool = mockk {
        every { this@mockk.apy } returns apy
        every { integrationId } returns StakingIntegrationID.P2PEthPool
    }

    private fun stakingAvailable(apy: BigDecimal): StakingAvailability =
        StakingAvailability.Available(option = stakingOption(apy))

    private fun stakeKitAvailable(vararg validatorList: Yield.Validator): StakingAvailability {
        // A real StakeKit option: stubbing `integrationId` on a mock would make mockk instrument
        // StakingIntegrationID.StakeKit, hitting the same enum-retransformation limitation.
        val yieldModel: Yield = mockk {
            every { validators } returns validatorList.toList()
            every { apy } returns BigDecimal("0.10")
            every { token } returns mockk()
            every { isAvailable } returns true
        }
        return StakingAvailability.Available(
            option = StakingOption.StakeKit(
                integrationId = StakingIntegrationID.StakeKit.Coin.Ton,
                yield = yieldModel,
            ),
        )
    }

    private fun validator(address: String, preferred: Boolean, rate: BigDecimal): Yield.Validator = mockk {
        every { this@mockk.address } returns address
        every { this@mockk.preferred } returns preferred
        every { rewardInfo } returns RewardInfo(rate = rate, type = RewardType.APY)
    }

    /** An active StakeKit balance whose items point at the given validator addresses. */
    private fun stakeKitBalance(vararg validatorAddresses: String?): StakingBalance.Data.StakeKit = mockk {
        every { balance } returns mockk {
            every { items } returns validatorAddresses.map { address ->
                mockk<BalanceItem> { every { validatorAddress } returns address }
            }
        }
    }

    /** Mirrors the production reward computation: `fiat * (yieldPercent / 100)`, rendered per year. */
    private fun expectedPerYearReward(fiat: BigDecimal, yieldPercent: BigDecimal) =
        fiat.multiply(yieldPercent.divide(BigDecimal("100"), RoundingMode.HALF_UP)).expectedPerYearText()

    private fun BigDecimal.expectedPerYearText() = resourceReference(
        R.string.for_you_earn_per_year,
        wrappedList(format { fiat(fiatCurrencySymbol = appCurrency.symbol, fiatCurrencyCode = appCurrency.code) }),
    )
}