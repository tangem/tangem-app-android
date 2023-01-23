package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent

sealed class MyWallets(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("My Wallets", event, params) {

    class MyWalletsScreenOpened : MyWallets(event = "My Wallets Screen Opened")
    class CardWasScanned : MyWallets(event = "Card Was Scanned")

    sealed class Button {
        class ScanNewCard : MyWallets(event = "Button - Scan New Card")
        class UnlockWithBiometrics : MyWallets(event = "Button - Unlock all with Face ID")
        class EditWalletTapped : MyWallets(event = "Button - Edit Wallet Tapped")
        class DeleteWalletTapped : MyWallets(event = "Button - Delete Wallet Tapped")
        class WalletUnlockTapped : MyWallets(event = "Button - Wallet Unlock Tapped")
    }
}
