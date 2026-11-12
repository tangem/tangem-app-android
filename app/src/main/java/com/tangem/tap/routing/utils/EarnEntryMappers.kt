package com.tangem.tap.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.EarnEntryStart

internal fun AppRoute.EarnEntry.Source.toEarnEntrySource(): EarnEntrySource = when (this) {
    AppRoute.EarnEntry.Source.TokenPage -> EarnEntrySource.TokenPage
    AppRoute.EarnEntry.Source.Markets -> EarnEntrySource.Markets
    AppRoute.EarnEntry.Source.Deeplink -> EarnEntrySource.Deeplink
    AppRoute.EarnEntry.Source.EarnBanner -> EarnEntrySource.EarnBanner
    AppRoute.EarnEntry.Source.YieldSheet -> EarnEntrySource.YieldSheet
    AppRoute.EarnEntry.Source.ForYou -> EarnEntrySource.ForYou
}

internal fun AppRoute.EarnEntry.Start.toEarnEntryStart(): EarnEntryStart = when (this) {
    AppRoute.EarnEntry.Start.Auto -> EarnEntryStart.Auto
    is AppRoute.EarnEntry.Start.Picker -> EarnEntryStart.Picker(preselected = preselected)
    is AppRoute.EarnEntry.Start.Info -> EarnEntryStart.Info(productId = productId)
}