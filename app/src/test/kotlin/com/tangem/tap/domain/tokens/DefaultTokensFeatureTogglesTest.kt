package com.tangem.tap.domain.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Pins which toggles gate the balance sources. The manager mock is deliberately NOT relaxed, so reading any other
 * toggle fails the test outright.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTokensFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultTokensFeatureToggles(featureTogglesManager = featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureTogglesManager)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN pluggable sources and yield positions toggles WHEN isYieldPositionsSourceEnabled THEN both are required`(
        model: YieldPositionsModel,
    ) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED)
        } returns model.isPluggableEnabled
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1948_YIELD_CONTRIBUTION_ENABLED)
        } returns model.isYieldPositionsEnabled

        // Act
        val actual = toggles.isYieldContributionEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN the toggles WHEN isYieldPositionsSourceEnabled read twice THEN it is re-evaluated each time`() {
        // Arrange — a getter, not a cached value: flipping the toggle at runtime must be observed
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED)
        } returns true
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1948_YIELD_CONTRIBUTION_ENABLED)
        } returnsMany listOf(false, true)

        // Act & Assert
        assertThat(toggles.isYieldContributionEnabled).isFalse()
        assertThat(toggles.isYieldContributionEnabled).isTrue()
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        YieldPositionsModel(isPluggableEnabled = false, isYieldPositionsEnabled = false, expected = false),
        YieldPositionsModel(isPluggableEnabled = false, isYieldPositionsEnabled = true, expected = false),
        YieldPositionsModel(isPluggableEnabled = true, isYieldPositionsEnabled = false, expected = false),
        YieldPositionsModel(isPluggableEnabled = true, isYieldPositionsEnabled = true, expected = true),
    )

    data class YieldPositionsModel(
        val isPluggableEnabled: Boolean,
        val isYieldPositionsEnabled: Boolean,
        val expected: Boolean,
    )
}