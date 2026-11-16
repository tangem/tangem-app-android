package com.tangem.features.earn.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultEarnFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultEarnFeatureToggles(featureTogglesManager)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN earn toggle WHEN read isEarnEnabled THEN reflects toggle`(isEarnEnabled: Boolean) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1676_EARN_ENABLED)
        } returns isEarnEnabled

        // Act
        val actual = toggles.isEarnEnabled

        // Assert
        assertThat(actual).isEqualTo(isEarnEnabled)
    }

    @Test
    fun `GIVEN toggle changes between reads WHEN read isEarnEnabled twice THEN each read reflects current value`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1676_EARN_ENABLED)
        } returnsMany listOf(false, true)

        // Act
        val first = toggles.isEarnEnabled
        val second = toggles.isEarnEnabled

        // Assert
        assertThat(listOf(first, second)).containsExactly(false, true).inOrder()
    }
}