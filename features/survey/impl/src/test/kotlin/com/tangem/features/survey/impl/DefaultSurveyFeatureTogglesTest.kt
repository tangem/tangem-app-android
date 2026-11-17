package com.tangem.features.survey.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultSurveyFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultSurveyFeatureToggles(featureTogglesManager)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN the NPS toggle WHEN isNpsSurveyEnabled read THEN it follows the toggle`(isEnabled: Boolean) {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1898_NPS_SURVEY_ENABLED) } returns isEnabled

        // Act
        val actual = toggles.isNpsSurveyEnabled

        // Assert
        assertThat(actual).isEqualTo(isEnabled)
    }
}