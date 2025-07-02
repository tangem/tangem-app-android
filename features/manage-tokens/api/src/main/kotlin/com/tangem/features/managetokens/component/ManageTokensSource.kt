package com.tangem.features.managetokens.component

enum class ManageTokensSource(val analyticsName: String) {
    STORIES(analyticsName = "Stories"),
    ONBOARDING(analyticsName = "Onboarding"),
    SETTINGS(analyticsName = "Settings"),
    SEND_VIA_SWAP(analyticsName = "SendViaSwap"),
}