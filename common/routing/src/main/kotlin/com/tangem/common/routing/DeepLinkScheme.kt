package com.tangem.common.routing

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
    Https(scheme = "https"),
}