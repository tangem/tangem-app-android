package com.tangem.data.settings

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSettingsFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultSettingsFeatureToggles(featureTogglesManager = featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureTogglesManager)
    }

    @Test
    fun `GIVEN toggle enabled WHEN isGeoCountryCacheFallbackEnabled THEN returns true`() {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(TOGGLE) } returns true

        // Act & Assert
        assertThat(toggles.isGeoCountryCacheFallbackEnabled).isTrue()
        verify(exactly = 1) { featureTogglesManager.isFeatureEnabled(TOGGLE) }
    }

    @Test
    fun `GIVEN toggle disabled WHEN isGeoCountryCacheFallbackEnabled THEN returns false`() {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(TOGGLE) } returns false

        // Act & Assert
        assertThat(toggles.isGeoCountryCacheFallbackEnabled).isFalse()
        verify(exactly = 1) { featureTogglesManager.isFeatureEnabled(TOGGLE) }
    }

    private companion object {
        val TOGGLE = FeatureToggles.AND_17136_GEO_COUNTRY_CACHE_FALLBACK_ENABLED
    }
}