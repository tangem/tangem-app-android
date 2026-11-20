package com.tangem.core.analytics.models.event

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class OnboardingAnalyticsEventTest {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN any selected wallet relation WHEN ImportWalletRequest built THEN the event matches the spec`(
        isSelectedWalletUnrelated: Boolean,
    ) {
        // Act
        val actual = OnboardingAnalyticsEvent.Backup.ImportWalletRequest(
            cloudBackup = AnalyticsParam.CloudBackupAvailability.Available,
            isSelectedWalletUnrelated = isSelectedWalletUnrelated,
        )

        // Assert
        assertThat(actual.id).isEqualTo("[Onboarding / Backup] Import Wallet Request")
        assertThat(actual.params).isEqualTo(mapOf("Cloud Backup" to "Available"))
    }

    @Test
    fun `GIVEN the add new wallet source WHEN ButtonMobileWallet built THEN the event matches the spec`() {
        // Act
        val actual = OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(
            source = AnalyticsParam.ScreensSources.AddNewWallet.value,
        )

        // Assert
        assertThat(actual.id).isEqualTo("[Onboarding] Button - Mobile Wallet")
        assertThat(actual.params).isEqualTo(mapOf("Source" to "Add New Wallet"))
    }
}