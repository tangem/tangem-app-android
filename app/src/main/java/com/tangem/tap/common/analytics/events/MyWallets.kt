package com.tangem.tap.common.analytics.events

sealed class MyWallets(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("My Wallets", event, params) {

    object MyWalletsScreenOpened : MyWallets(event = "My Wallets Screen Opened")
    object CardWasScanned : MyWallets(event = "Card Was Scanned")
    object WalletUnlockTapped : MyWallets(event = "Wallet Unlock Tapped")

    object Button {
        object ScanNewCard : MyWallets(event = "Button - Scan New Card")
        object UnlockWithBiometrics : MyWallets(event = "Button - Unlock all with Face ID")
        object EditWalletTapped : MyWallets(event = "Button - Edit Wallet Tapped")
        object DeleteWalletTapped : MyWallets(event = "Button - Delete Wallet Tapped")
    }
}
