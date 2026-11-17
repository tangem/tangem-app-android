package com.tangem.core.navigation.deeplink

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
    Https(scheme = "https"),
}