package com.tangem.tap.domain.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Pins which toggles gate the balance sources. The manager mock is deliberately NOT relaxed, so reading any other
 * toggle fails the test outright.
 */
internal class DefaultTokensFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultTokensFeatureToggles(featureTogglesManager = featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureTogglesManager)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN yield contribution toggle WHEN isYieldContributionEnabled THEN follows the toggle`(isEnabled: Boolean) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1948_YIELD_CONTRIBUTION_ENABLED)
        } returns isEnabled

        // Act
        val actual = toggles.isYieldContributionEnabled

        // Assert
        assertThat(actual).isEqualTo(isEnabled)
    }

    @Test
    fun `GIVEN the toggles WHEN isYieldContributionEnabled read twice THEN it is re-evaluated each time`() {
        // Arrange — a getter, not a cached value: flipping the toggle at runtime must be observed
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1948_YIELD_CONTRIBUTION_ENABLED)
        } returnsMany listOf(false, true)

        // Act & Assert
        assertThat(toggles.isYieldContributionEnabled).isFalse()
        assertThat(toggles.isYieldContributionEnabled).isTrue()
    }
}