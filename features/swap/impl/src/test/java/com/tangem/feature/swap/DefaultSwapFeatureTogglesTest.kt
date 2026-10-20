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

    @Test
    fun `GIVEN geo blocking toggle enabled WHEN read isExpressCategoriesGeoBlockingEnabled THEN true`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED)
        } returns true

        // Act
        val actual = toggles.isExpressCategoriesGeoBlockingEnabled

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN geo blocking toggle disabled WHEN read isExpressCategoriesGeoBlockingEnabled THEN false`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED)
        } returns false

        // Act
        val actual = toggles.isExpressCategoriesGeoBlockingEnabled

        // Assert
        assertThat(actual).isFalse()
    }

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