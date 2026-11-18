package com.tangem.feature.wallet.presentation.wallet.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test

internal class WalletScreenAnalyticsEventTest {

    @Test
    fun `GIVEN permanent upgrade banner WHEN notice built THEN it matches the spec`() {
        // Act
        val actual = WalletScreenAnalyticsEvent.MainScreen.NoticePermanentUpgradeBanner(UserWalletId("011"))

        // Assert
        assertThat(actual.id).isEqualTo("[Main Screen] Notice - Permanent Upgrade Banner")
        assertThat(actual.params).isEmpty()
    }

    @Test
    fun `GIVEN two wallets WHEN notices built THEN each wallet is limited to one notice per session separately`() {
        // Act
        val first = WalletScreenAnalyticsEvent.MainScreen.NoticePermanentUpgradeBanner(UserWalletId("011"))
        val sameWallet = WalletScreenAnalyticsEvent.MainScreen.NoticePermanentUpgradeBanner(UserWalletId("011"))
        val otherWallet = WalletScreenAnalyticsEvent.MainScreen.NoticePermanentUpgradeBanner(UserWalletId("022"))

        // Assert
        assertThat(sameWallet.oneTimeEventId).isEqualTo(first.oneTimeEventId)
        assertThat(otherWallet.oneTimeEventId).isNotEqualTo(first.oneTimeEventId)
    }
}