package com.tangem.common.routing

sealed class DeepLinkRoute {

    abstract val host: String

    data object Onramp : DeepLinkRoute() {
        override val host: String = "onramp"
    }

    data object Sell : DeepLinkRoute() {
        override val host: String = "redirect_sell"
    }

    data object Buy : DeepLinkRoute() {
        override val host: String = "redirect"
    }

    data object Referral : DeepLinkRoute() {
        override val host: String = "referral"
    }
}

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
}