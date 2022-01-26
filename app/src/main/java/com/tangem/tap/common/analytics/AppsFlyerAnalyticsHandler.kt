package com.tangem.tap.common.analytics

import android.app.Application
import com.appsflyer.AppsFlyerLib
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError

class AppsFlyerAnalyticsHandler(val context: Application): AnalyticsHandler() {

    override fun triggerEvent(
        event: AnalyticsEvent,
        card: Card?,
        blockchain: String?,
        params: Map<String, String>
    ) {
        AppsFlyerLib.getInstance().logEvent(context,
            event.event, prepareParams(card, blockchain))
    }

    override fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>?,
        card: Card?
    ) {
// [REDACTED_TODO_COMMENT]
    }

    override fun logError(error: Throwable, params: Map<String, String>) {
// [REDACTED_TODO_COMMENT]
    }

}