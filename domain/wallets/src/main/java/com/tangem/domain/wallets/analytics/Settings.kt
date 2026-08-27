package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent

sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    class ScreenOpened : Settings(event = "Settings Screen Opened")

    class ButtonManageTokens : Settings(event = "Button - Manage Tokens")

    class ButtonOpenChat : Settings(event = "Button - Open Chat")

    class ButtonAddHardwareWallet(
        walletsType: AnalyticsParam.WalletsType?,
    ) : Settings(
        event = "Button - Add Hardware Wallet",
        params = mapOf(AnalyticsParam.WALLETS to walletsType.paramValue()),
    )

    class ButtonAddMobileWallet(
        walletsType: AnalyticsParam.WalletsType?,
    ) : Settings(
        event = "Button - Add Mobile Wallet",
        params = mapOf(AnalyticsParam.WALLETS to walletsType.paramValue()),
    )

    class NoticeMoreMobileWallets(
        walletsType: AnalyticsParam.WalletsType?,
    ) : Settings(
        event = "Notice - More Mobile Wallets",
        params = mapOf(AnalyticsParam.WALLETS to walletsType.paramValue()),
    )

    class ColdWalletAdded(
        source: AnalyticsParam.ScreensSources?,
    ) : Settings(
        event = "Cold Wallet Added",
        params = mapOf(AnalyticsParam.SOURCE to (source?.value ?: "Unknown")),
    ), OneTimeAnalyticsEvent, AppsFlyerIncludedEvent {
        override val oneTimeEventId: String = id
    }
}

private fun AnalyticsParam.WalletsType?.paramValue(): String = this?.value ?: "Unknown"