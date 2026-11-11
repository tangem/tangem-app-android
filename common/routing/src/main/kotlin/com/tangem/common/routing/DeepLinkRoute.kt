package com.tangem.common.routing

sealed class DeepLinkRoute {

    abstract val host: String

    data object TokenDetails : DeepLinkRoute() {
        override val host: String = "token"
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