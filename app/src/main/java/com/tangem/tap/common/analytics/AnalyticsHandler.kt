package com.tangem.tap.common.analytics

import com.tangem.commands.common.card.Card


interface AnalyticsHandler {
    fun triggerEvent(event: AnalyticsEvent, card: Card? = null)
}