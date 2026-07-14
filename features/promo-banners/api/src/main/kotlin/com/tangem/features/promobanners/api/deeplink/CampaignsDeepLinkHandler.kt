package com.tangem.features.promobanners.api.deeplink

interface CampaignsDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): CampaignsDeepLinkHandler
    }
}