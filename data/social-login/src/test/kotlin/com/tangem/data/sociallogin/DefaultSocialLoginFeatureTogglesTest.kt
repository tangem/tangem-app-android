package com.tangem.data.sociallogin

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialLoginFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()

    private val toggles = DefaultSocialLoginFeatureToggles(
        featureTogglesManager = featureTogglesManager,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN social login toggle WHEN isSocialLoginEnabled THEN follows the toggle`(model: Model) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1555_SOCIAL_LOGIN_ENABLED)
        } returns model.isSocialLoginToggleOn

        // Act
        val actual = toggles.isSocialLoginEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val isSocialLoginToggleOn: Boolean, val expected: Boolean)

    private fun provideTestModels() = listOf(
        Model(isSocialLoginToggleOn = true, expected = true),
        Model(isSocialLoginToggleOn = false, expected = false),
    )
}