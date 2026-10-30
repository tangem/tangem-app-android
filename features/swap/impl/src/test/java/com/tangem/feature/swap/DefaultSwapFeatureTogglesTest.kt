package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class DefaultSwapFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultSwapFeatureToggles(featureTogglesManager)

    @Test
    fun `GIVEN Tron gasless swap toggle enabled WHEN read isTronGaslessSwapEnabled THEN true`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1803_TRON_GASLESS_SWAP_ENABLED)
        } returns true

        // Act
        val actual = toggles.isTronGaslessSwapEnabled

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN Tron gasless swap toggle disabled WHEN read isTronGaslessSwapEnabled THEN false`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1803_TRON_GASLESS_SWAP_ENABLED)
        } returns false

        // Act
        val actual = toggles.isTronGaslessSwapEnabled

        // Assert
        assertThat(actual).isFalse()
    }

    @ParameterizedTest
    @CsvSource(
        "true, true, true",
        "true, false, false",
        "false, true, false",
        "false, false, false",
    )
    fun `GIVEN swap and DEX toggles WHEN read isTronGaslessDexSwapEnabled THEN both are required`(
        isSwapEnabled: Boolean,
        isDexEnabled: Boolean,
        expected: Boolean,
    ) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1803_TRON_GASLESS_SWAP_ENABLED)
        } returns isSwapEnabled
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1850_TRON_GASLESS_DEX_SWAP_ENABLED)
        } returns isDexEnabled

        // Act
        val actual = toggles.isTronGaslessDexSwapEnabled

        // Assert
        assertThat(actual).isEqualTo(expected)
    }
}