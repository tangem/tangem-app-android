package com.tangem.common.routing

sealed class DeepLinkRoute {

    abstract val host: String

    data object Onramp : DeepLinkRoute() {
        override val host: String = "onramp"
    }
}

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
}