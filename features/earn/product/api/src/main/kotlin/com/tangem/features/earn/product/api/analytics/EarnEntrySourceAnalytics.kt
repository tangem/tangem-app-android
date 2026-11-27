package com.tangem.features.earn.product.api.analytics

import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.product.api.navigation.EarnFlowOpener

fun EarnEntrySource.toAnalyticsValue(): String = when (this) {
    EarnEntrySource.TokenPage -> "Token Page"
    EarnEntrySource.Markets -> "Markets"
    EarnEntrySource.Deeplink -> "Deeplink"
    EarnEntrySource.EarnBanner -> "Earn Banner"
    EarnEntrySource.YieldSheet -> "Yield Sheet"
    EarnEntrySource.ForYou -> "For You"
}

fun EarnFlowOpener.toAnalyticsValue(): String = when (this) {
    EarnFlowOpener.YieldPicker -> "Yield Picker"
    EarnFlowOpener.InfoScreen -> "Info Screen"
}