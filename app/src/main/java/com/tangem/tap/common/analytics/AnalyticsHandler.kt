package com.tangem.tap.common.analytics

import com.tangem.TangemSdkError
import com.tangem.blockchain.common.Blockchain
import com.tangem.commands.common.card.Card


interface AnalyticsHandler {
    fun triggerEvent(event: AnalyticsEvent, card: Card? = null, blockchain: Blockchain? = null)
    fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: FirebaseAnalyticsHandler.ActionToLog,
        parameters: Map<FirebaseAnalyticsHandler.AnalyticsParam, String>? = null,
        card: Card? = null
    )
}