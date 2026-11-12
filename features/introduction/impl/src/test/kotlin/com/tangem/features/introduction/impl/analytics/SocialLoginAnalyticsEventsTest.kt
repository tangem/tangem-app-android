package com.tangem.features.introduction.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import org.junit.jupiter.api.Test

internal class SocialLoginAnalyticsEventsTest {

    @Test
    fun `GIVEN Google and the add new wallet source WHEN ButtonContinue built THEN the event matches the spec`() {
        // Act
        val actual = SocialLoginAnalyticsEvents.ButtonContinue(
            provider = SocialLoginProvider.Google,
            source = AnalyticsParam.ScreensSources.AddNewWallet,
        )

        // Assert
        assertThat(actual.id).isEqualTo("[Onboarding / Social login] Button - Continue with Google")
        assertThat(actual.params).isEqualTo(mapOf("Source" to "Add New Wallet", "Product Type" to "Mobile Wallet"))
    }

    @Test
    fun `GIVEN an unsupported OS WHEN MinOsNotSupported built THEN the event matches the spec`() {
        // Act
        val actual = SocialLoginAnalyticsEvents.MinOsNotSupported()

        // Assert
        assertThat(actual.id).isEqualTo("[Onboarding / Social login] Notice - Min OS Not Supported")
        assertThat(actual.params).isEqualTo(mapOf("Product Type" to "Mobile Wallet"))
    }
}