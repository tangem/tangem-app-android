package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

/**
 * The "Coming soon" popup shown for an action joint accounts do not support yet. It lives here rather than among the
 * joint account feature events because the shared token actions in `common` show it too.
 */
class JointAccountFakedoorAnalyticsEvent(
    source: Source,
    action: Action,
    blockchain: String? = null,
    token: String? = null,
) : AnalyticsEvent(
    category = "Joint Account",
    event = "Fakedoor Popup Displayed",
    params = buildMap {
        put(AnalyticsParam.SOURCE, source.value)
        put(AnalyticsParam.ACTION, action.value)
        blockchain?.let { put(AnalyticsParam.BLOCKCHAIN, it) }
        token?.let { put(AnalyticsParam.TOKEN_PARAM, it) }
    },
) {

    enum class Source(val value: String) {
        WalletConnect("WalletConnect"),
        Transfer("Transfer"),
        MainScreenSwap("Main Screen Swap"),
    }

    enum class Action(val value: String) {
        Connect("Connect"),
        Swap("Swap"),
        SwapAndSend("Swap&Send"),
        Sell("Sell"),
    }
}