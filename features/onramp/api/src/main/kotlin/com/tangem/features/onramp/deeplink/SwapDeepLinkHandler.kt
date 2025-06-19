package com.tangem.features.onramp.deeplink

interface SwapDeepLinkHandler {

    interface Factory {
        fun create(): SwapDeepLinkHandler
    }
}