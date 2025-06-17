package com.tangem.features.onramp.deeplink

interface SellDeepLinkHandler {

    interface Factory {
        fun create(): SellDeepLinkHandler
    }
}