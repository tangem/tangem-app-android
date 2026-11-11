package com.tangem.common.routing

sealed class DeepLinkRoute {

    abstract val host: String

    data object TangemPayOnboard : DeepLinkRoute() {
        override val host: String = "onboard-visa"
    }

    data object PayApp : DeepLinkRoute() {
        override val host: String = "tangem.com"
    }

    data object PayAppMain : DeepLinkRoute() {
        override val host: String = "pay-app-main"
    }
}

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
    Https(scheme = "https"),
}