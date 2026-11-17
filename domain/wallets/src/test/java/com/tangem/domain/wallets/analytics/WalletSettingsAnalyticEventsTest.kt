package com.tangem.domain.wallets.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.TrackingContextExcludedEvent
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletSettingsAnalyticEventsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an event about a wallet that is gone WHEN built THEN it names the mobile wallet itself`(
        event: AnalyticsEvent,
    ) {
        // Assert
        assertThat(event).isInstanceOf(TrackingContextExcludedEvent::class.java)
        assertThat(event.params).containsEntry(
            AnalyticsParam.PRODUCT_TYPE,
            AnalyticsParam.ProductType.MobileWallet.value,
        )
    }

    @Test
    fun `GIVEN an unbind failed on the server WHEN DeleteWalletError built THEN the message is readable`() {
        // Act
        val actual = WalletSettingsAnalyticEvents.DeleteWalletError(SocialLoginError.ServerError())

        // Assert
        assertThat(actual.params).containsEntry(AnalyticsParam.ERROR_MESSAGE, "Server error")
    }

    @ParameterizedTest
    @EnumSource(
        value = AnalyticsParam.ScreensSources::class,
        names = ["UpgradeBanner", "PermanentBanner", "WalletSettings", "Backup"],
    )
    fun `GIVEN an entry point WHEN HardwareBackupScreenOpened built THEN the source is reported`(
        source: AnalyticsParam.ScreensSources,
    ) {
        // Arrange
        val expectedSource = when (source) {
            AnalyticsParam.ScreensSources.UpgradeBanner -> "Upgrade Banner"
            AnalyticsParam.ScreensSources.PermanentBanner -> "Permanent Banner"
            AnalyticsParam.ScreensSources.WalletSettings -> "Wallet Settings"
            AnalyticsParam.ScreensSources.Backup -> "Backup"
            else -> error("Not a hardware upgrade entry point: $source")
        }

        // Act
        val actual = WalletSettingsAnalyticEvents.HardwareBackupScreenOpened(source = source)

        // Assert
        assertThat(actual.id).isEqualTo("[Settings / Wallet Settings] Hardware Backup Screen Opened")
        assertThat(actual.params).isEqualTo(mapOf("Source" to expectedSource))
    }

    private fun provideTestModels(): List<AnalyticsEvent> = listOf(
        WalletSettingsAnalyticEvents.SocialRecoveryDeleted(),
        WalletSettingsAnalyticEvents.SocialRecoveryDeletionError(SocialLoginError.CloudUnavailable),
        WalletSettingsAnalyticEvents.WalletForgotten(),
        WalletSettingsAnalyticEvents.WalletLoggedOut(),
        WalletSettingsAnalyticEvents.WalletDeleted(),
        WalletSettingsAnalyticEvents.DeleteWalletError(errorMessage = "Local deletion error"),
    )
}