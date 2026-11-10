package com.tangem.features.marketing.impl.promopage

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPromoPageDeepLinkHandlerTest {

    private val marketingFeatureToggles: MarketingFeatureToggles = mockk()

    private val handler = DefaultPromoPageDeepLinkHandler(marketingFeatureToggles = marketingFeatureToggles)

    @ParameterizedTest
    @ProvideTestModels
    fun isEnabled(model: IsEnabledModel) {
        // Arrange
        every { marketingFeatureToggles.isPromoPageEnabled } returns model.isPromoPageEnabled

        // Act
        val actual = handler.isEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class IsEnabledModel(val isPromoPageEnabled: Boolean, val expected: Boolean)

    private fun provideTestModels() = listOf(
        IsEnabledModel(isPromoPageEnabled = true, expected = true),
        IsEnabledModel(isPromoPageEnabled = false, expected = false),
    )
}