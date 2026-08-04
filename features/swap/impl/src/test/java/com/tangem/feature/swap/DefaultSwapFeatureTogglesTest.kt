package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class DefaultSwapFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultSwapFeatureToggles(featureTogglesManager)

    @Test
    fun `GIVEN pulse toggle enabled WHEN read isChooseTokenPulseEnabled THEN true`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16357_CHOOSE_TOKEN_PULSE_ANIMATION)
        } returns true

        // Act
        val actual = toggles.isChooseTokenPulseEnabled

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN pulse toggle disabled WHEN read isChooseTokenPulseEnabled THEN false`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16357_CHOOSE_TOKEN_PULSE_ANIMATION)
        } returns false

        // Act
        val actual = toggles.isChooseTokenPulseEnabled

        // Assert
        assertThat(actual).isFalse()
    }
}