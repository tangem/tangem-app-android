package com.tangem.features.wallet.deeplink

interface WalletDeepLinkHandler {

    interface Factory {
        fun create(): WalletDeepLinkHandler
    }
}