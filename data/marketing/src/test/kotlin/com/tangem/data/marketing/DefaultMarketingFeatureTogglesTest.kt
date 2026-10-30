package com.tangem.data.marketing

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketingFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultMarketingFeatureToggles(featureTogglesManager = featureTogglesManager)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN binding toggle WHEN isUidBindingEnabled THEN follows the toggle`(model: Model) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1732_MARKETING_UID_BINDING_ENABLED)
        } returns model.isToggleOn

        // Act
        val actual = toggles.isUidBindingEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN promo page toggle WHEN isPromoPageEnabled THEN follows the toggle`(model: Model) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1732_PROMO_PAGE_ENABLED)
        } returns model.isToggleOn

        // Act
        val actual = toggles.isPromoPageEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val isToggleOn: Boolean, val expected: Boolean)

    private fun provideTestModels() = listOf(
        Model(isToggleOn = true, expected = true),
        Model(isToggleOn = false, expected = false),
    )
}