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

    private fun provideTestModels(): List<AnalyticsEvent> = listOf(
        WalletSettingsAnalyticEvents.SocialRecoveryDeleted(),
        WalletSettingsAnalyticEvents.SocialRecoveryDeletionError(SocialLoginError.CloudUnavailable),
        WalletSettingsAnalyticEvents.WalletForgotten(),
        WalletSettingsAnalyticEvents.WalletLoggedOut(),
        WalletSettingsAnalyticEvents.WalletDeleted(),
        WalletSettingsAnalyticEvents.DeleteWalletError(errorMessage = "Local deletion error"),
    )
}