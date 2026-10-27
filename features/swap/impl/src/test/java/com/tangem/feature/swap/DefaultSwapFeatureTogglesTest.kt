package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultSwapFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultSwapFeatureToggles(featureTogglesManager)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN DEX toggle WHEN read isTronGaslessDexSwapEnabled THEN reflects toggle`(isDexEnabled: Boolean) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1850_TRON_GASLESS_DEX_SWAP_ENABLED)
        } returns isDexEnabled

        // Act
        val actual = toggles.isTronGaslessDexSwapEnabled

        // Assert
        assertThat(actual).isEqualTo(isDexEnabled)
    }
}