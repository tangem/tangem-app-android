package com.tangem.feature.wallet.link

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class WalletLinkAnalyticsEvent(
    event: String,
) : AnalyticsEvent(category = "Backend Auth", event = event) {

    class SheetShown : WalletLinkAnalyticsEvent(event = "Link Wallet Sheet Shown")

    class ButtonLinkWallet : WalletLinkAnalyticsEvent(event = "Button - Link Wallet")

    class Canceled : WalletLinkAnalyticsEvent(event = "Link Wallet Canceled")
}