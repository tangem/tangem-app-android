package com.tangem.tap.common.analytics

import com.tangem.commands.Card

interface AnalyticsHandler {
    fun triggerEvent(event: AnalyticsEvent, card: Card?)
}