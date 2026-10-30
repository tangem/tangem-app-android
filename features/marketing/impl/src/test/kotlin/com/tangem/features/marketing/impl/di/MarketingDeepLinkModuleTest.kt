package com.tangem.features.marketing.impl.di

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.marketing.MarketingFeatureToggles
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class MarketingDeepLinkModuleTest {

    private val marketingFeatureToggles: MarketingFeatureToggles = mockk()

    private val handler = MarketingDeepLinkModule.providePromoPageDeepLinkHandler(marketingFeatureToggles)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN promo page toggle WHEN isEnabled read THEN it follows the toggle`(isToggleOn: Boolean) {
        // Arrange
        every { marketingFeatureToggles.isPromoPageEnabled } returns isToggleOn

        // Act
        val actual = handler.isEnabled

        // Assert
        assertThat(actual).isEqualTo(isToggleOn)
    }
}