package com.tangem.data.marketing.featuretoggle

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class DefaultMarketingFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val featureToggles = DefaultMarketingFeatureToggles(featureTogglesManager)

    @Test
    fun `GIVEN toggle enabled WHEN isMarketingBannersEnabled THEN true`() {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15862_MARKETING_BANNERS_ENABLED) } returns true

        // Assert
        assertThat(featureToggles.isMarketingBannersEnabled).isTrue()
    }

    @Test
    fun `GIVEN toggle disabled WHEN isMarketingBannersEnabled THEN false`() {
        // Arrange
        every { featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15862_MARKETING_BANNERS_ENABLED) } returns false

        // Assert
        assertThat(featureToggles.isMarketingBannersEnabled).isFalse()
    }
}