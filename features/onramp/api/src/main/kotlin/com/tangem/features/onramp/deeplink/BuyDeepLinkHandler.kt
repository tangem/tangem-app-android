package com.tangem.features.onramp.deeplink

interface BuyDeepLinkHandler {

    interface Factory {
        fun create(): BuyDeepLinkHandler
    }
}