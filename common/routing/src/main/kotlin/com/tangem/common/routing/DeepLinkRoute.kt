package com.tangem.common.routing

sealed class DeepLinkRoute {

    abstract val host: String

    data object Onramp : DeepLinkRoute() {
        override val host: String = "onramp"
    }

    data object SellRedirect : DeepLinkRoute() {
        override val host: String = "redirect_sell"
    }

    data object BuyRedirect : DeepLinkRoute() {
        override val host: String = "redirect"
    }

    data object Buy : DeepLinkRoute() {
        override val host: String = "buy"
    }

    data object TokenDetails : DeepLinkRoute() {
        override val host: String = "token"
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

    data object TangemPayOnboard : DeepLinkRoute() {
        override val host: String = "onboard-visa"
    }

    data object PayApp : DeepLinkRoute() {
        override val host: String = "tangem.com"
    }

    data object PayAppMain : DeepLinkRoute() {
        override val host: String = "pay-app-main"
    }

    data object PromoPage : DeepLinkRoute() {
        override val host: String = "promo_page"
    }

    data object McpAuthorization : DeepLinkRoute() {
        override val host: String = "mcp.authorization"
    }

    data object JointAccount : DeepLinkRoute() {
        override val host: String = "joint_account"

        const val ENTRY_MEMBERS: String = "members"
        const val ENTRY_OVERVIEW: String = "overview"
    }
}

enum class DeepLinkScheme(val scheme: String) {
    Tangem(scheme = "tangem"),
    WalletConnect(scheme = "wc"),
    Https(scheme = "https"),
}