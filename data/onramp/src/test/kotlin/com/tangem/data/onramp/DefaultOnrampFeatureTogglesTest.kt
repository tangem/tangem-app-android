package com.tangem.data.onramp

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
internal class DefaultOnrampFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val toggles = DefaultOnrampFeatureToggles(featureTogglesManager = featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureTogglesManager)
    }

    @Test
    fun `GIVEN toggle enabled WHEN isThemedPaymentMethodImagesEnabled THEN returns true`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16359_ONRAMP_THEMED_PAYMENT_METHOD_IMAGES)
        } returns true

        // Act & Assert
        assertThat(toggles.isThemedPaymentMethodImagesEnabled).isTrue()
        verify(exactly = 1) {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16359_ONRAMP_THEMED_PAYMENT_METHOD_IMAGES)
        }
    }

    @Test
    fun `GIVEN toggle disabled WHEN isThemedPaymentMethodImagesEnabled THEN returns false`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16359_ONRAMP_THEMED_PAYMENT_METHOD_IMAGES)
        } returns false

        // Act & Assert
        assertThat(toggles.isThemedPaymentMethodImagesEnabled).isFalse()
    }

    @Test
    fun `GIVEN toggle enabled WHEN isExpressCategoriesGeoBlockingEnabled THEN returns true`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED)
        } returns true

        // Act & Assert
        assertThat(toggles.isExpressCategoriesGeoBlockingEnabled).isTrue()
        verify(exactly = 1) {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED)
        }
    }

    @Test
    fun `GIVEN toggle disabled WHEN isExpressCategoriesGeoBlockingEnabled THEN returns false`() {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED)
        } returns false

        // Act & Assert
        assertThat(toggles.isExpressCategoriesGeoBlockingEnabled).isFalse()
    }
}