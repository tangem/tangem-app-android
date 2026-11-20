package com.tangem.features.earn.product.api.analytics

import com.tangem.features.earn.api.EarnEntrySource

fun EarnEntrySource.toAnalyticsValue(): String = when (this) {
    EarnEntrySource.TokenPage -> "Token Page"
    EarnEntrySource.Markets -> "Markets"
    EarnEntrySource.Deeplink -> "Deeplink"
    EarnEntrySource.EarnBanner -> "Earn Banner"
    EarnEntrySource.YieldSheet -> "Yield Sheet"
    EarnEntrySource.ForYou -> "For You"
}