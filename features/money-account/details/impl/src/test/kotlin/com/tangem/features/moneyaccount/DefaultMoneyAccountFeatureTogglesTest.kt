package com.tangem.features.moneyaccount

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMoneyAccountFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()

    private val toggles = DefaultMoneyAccountFeatureToggles(
        featureTogglesManager = featureTogglesManager,
        tokensFeatureToggles = tokensFeatureToggles,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN yield mode and contribution toggles WHEN isYieldModeEnabled THEN on only when both are on`(
        model: YieldModeModel,
    ) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1525_MONEY_ACCOUNT_YIELD_MODE_ENABLED)
        } returns model.isYieldModeToggleOn
        every { tokensFeatureToggles.isYieldContributionEnabled } returns model.isContributionModeOn

        // Act
        val actual = toggles.isYieldModeEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class YieldModeModel(val isYieldModeToggleOn: Boolean, val isContributionModeOn: Boolean, val expected: Boolean)

    private fun provideTestModels() = listOf(
        YieldModeModel(isYieldModeToggleOn = true, isContributionModeOn = true, expected = true),
        YieldModeModel(isYieldModeToggleOn = true, isContributionModeOn = false, expected = false),
        YieldModeModel(isYieldModeToggleOn = false, isContributionModeOn = true, expected = false),
        YieldModeModel(isYieldModeToggleOn = false, isContributionModeOn = false, expected = false),
    )
}