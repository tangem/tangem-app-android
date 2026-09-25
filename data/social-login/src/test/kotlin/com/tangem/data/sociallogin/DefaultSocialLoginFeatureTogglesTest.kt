package com.tangem.data.sociallogin

import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialLoginFeatureTogglesTest {

    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val authFeatureToggles: AuthFeatureToggles = mockk()

    private val toggles = DefaultSocialLoginFeatureToggles(
        featureTogglesManager = featureTogglesManager,
        authFeatureToggles = authFeatureToggles,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN both toggles WHEN isSocialLoginEnabled THEN mobile authentication is required too`(model: Model) {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1555_SOCIAL_LOGIN_ENABLED)
        } returns model.isSocialLoginToggleOn
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns model.isBackendAuthOn

        // Act
        val actual = toggles.isSocialLoginEnabled

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val isSocialLoginToggleOn: Boolean, val isBackendAuthOn: Boolean, val expected: Boolean)

    private fun provideTestModels() = listOf(
        Model(isSocialLoginToggleOn = true, isBackendAuthOn = true, expected = true),
        // without mobile authentication the device key is a stub, so the flow could only dead-end
        Model(isSocialLoginToggleOn = true, isBackendAuthOn = false, expected = false),
        Model(isSocialLoginToggleOn = false, isBackendAuthOn = true, expected = false),
        Model(isSocialLoginToggleOn = false, isBackendAuthOn = false, expected = false),
    )
}