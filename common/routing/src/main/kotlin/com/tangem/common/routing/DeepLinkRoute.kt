package com.tangem.common.routing

sealed class DeepLinkRoute {

    abstract val host: String

    data object Onramp : DeepLinkRoute() {
        override val host: String = "onramp"
    }

    data object SellRedirect : DeepLinkRoute() {
        override val host: String = "redirect_sell"
    }

    data object Sell : DeepLinkRoute() {
        override val host: String = "sell"
    }

    data object BuyRedirect : DeepLinkRoute() {
        override val host: String = "redirect"
    }

    data object Buy : DeepLinkRoute() {
        override val host: String = "buy"
    }

    data object Referral : DeepLinkRoute() {
        override val host: String = "referral"
    }

    data object Wallet : DeepLinkRoute() {
        override val host: String = "main"
    }

    data object TokenDetails : DeepLinkRoute() {
        override val host: String = "token"
    }

    data object Staking : DeepLinkRoute() {
        override val host: String = "staking"
    }

    data object Markets : DeepLinkRoute() {
        override val host: String = "markets"
    }

    data object MarketTokenDetail : DeepLinkRoute() {
        override val host: String = "token_chart"
    }

    data object Swap : DeepLinkRoute() {
        override val host: String = "swap"
    }

    data object WalletConnect : DeepLinkRoute() {
        override val host: String = "wc"
    }
}

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
}