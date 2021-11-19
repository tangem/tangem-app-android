package com.tangem.tap.common.analytics

import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError


interface AnalyticsHandler {
    fun triggerEvent(event: AnalyticsEvent, card: Card? = null, blockchain: String? = null)
    fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: FirebaseAnalyticsHandler.ActionToLog,
        parameters: Map<FirebaseAnalyticsHandler.AnalyticsParam, String>? = null,
        card: Card? = null
    )
}