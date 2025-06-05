package com.tangem.features.markets.deeplink

interface MarketsDeepLinkHandler {

    interface Factory {
        fun create(): MarketsDeepLinkHandler
    }
}