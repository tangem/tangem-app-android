package com.tangem.feature.walletsettings.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS

internal sealed class WalletSettingsAnalyticEvents(
    category: String = "Settings / Wallet",
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    data class NftToggleSwitch(val enabled: AnalyticsParam.OnOffState) : WalletSettingsAnalyticEvents(
        event = "NFT toggle switch",
        params = mapOf(STATUS to enabled.value),
    )
}