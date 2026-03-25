package com.tangem.common

object TangemSiteShareUrlBuilder {

    private const val BASE_URL = "https://tangem.com"
    private const val CRYPTOCURRENCIES_PATH = "cryptocurrencies"

    fun shareUrl(tokenId: String): String {
        return "$BASE_URL/$CRYPTOCURRENCIES_PATH/$tokenId"
    }
}