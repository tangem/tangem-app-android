package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingOption
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
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
        fun `GIVEN null account status list WHEN convert THEN no-tokens state`() {
            // Arrange
            val converter = createConverter()

            // Act
            val result = converter.convert(null) as EarnOpportunitiesUM.Content

            // Assert
            assertThat(result.subtitleRes).isEqualTo(R.string.for_you_earn_opportunities_no_available_tokens)
        }

        @Test
        fun `GIVEN currencies without any earn option WHEN convert THEN no-tokens state`() {
            // Arrange — no yield map entries and no staking availability → nothing is earn-eligible
            val status = createStatus(createEarnCurrency(), createEarnStatusValue())
            val converter = createConverter()

            // Act
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

            // Assert — 100 * (10.00 / 100) = 10.00 per year, not 50
            assertThat((result as EarnOpportunitiesUM.Content).potentialReward)
                .isEqualTo(expectedPerYearReward(fiat = BigDecimal("100"), yieldPercent = BigDecimal("10.00")))
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
            val result = converter.convert(createAccountStatusList(createPortfolioStatus(listOf(status))))

            // Assert — 200 * 0.04 = 8 per year
            val expectedTotal = BigDecimal("200").multiply(BigDecimal("0.04"))
            assertThat((result as EarnOpportunitiesUM.Content).potentialReward)
                .isEqualTo(expectedTotal.expectedPerYearText())
        }
    }

    private fun createConverter(
        yieldSupplyAvailability: Map<String, BigDecimal> = emptyMap(),
        yieldStakingAvailability: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
        topEarnTokens: EarnTopToken? = null,
        isAccountsModeEnabled: Boolean = false,
    ) = ForYouEarnOpportunitiesConverter(
        appCurrency = appCurrency,
        isAccountsModeEnabled = isAccountsModeEnabled,
        expandedAssetIds = emptySet(),
        expandClick = {},
        yieldSupplyAvailability = yieldSupplyAvailability,
        yieldStakingAvailability = yieldStakingAvailability,
        topEarnTokens = topEarnTokens,
    )

    private fun stakingOption(apy: BigDecimal): StakingOption.P2PEthPool = mockk {
        every { this@mockk.apy } returns apy
    }

    private fun stakingAvailable(apy: BigDecimal): StakingAvailability =
        StakingAvailability.Available(option = stakingOption(apy))

    /** Mirrors the production reward computation: `fiat * (yieldPercent / 100)`, rendered per year. */
    private fun expectedPerYearReward(fiat: BigDecimal, yieldPercent: BigDecimal) =
        fiat.multiply(yieldPercent.divide(BigDecimal("100"), RoundingMode.HALF_UP)).expectedPerYearText()

    private fun BigDecimal.expectedPerYearText() = resourceReference(
        R.string.for_you_earn_per_year,
        wrappedList(format { fiat(fiatCurrencySymbol = appCurrency.symbol, fiatCurrencyCode = appCurrency.code) }),
    )
}