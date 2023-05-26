package com.tangem.tap.features.customtoken.impl.presentation.viewmodels

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.common.analytics.events.ManageTokens

/** Analytics sender for tokens list screen */
class AddCustomTokenAnalyticsSender(private val analyticsEventHandler: AnalyticsEventHandler) {

    fun sendWhenScreenOpened() {
        analyticsEventHandler.send(ManageTokens.CustomToken.ScreenOpened)
    }

    fun sendWhenAddTokenButtonClicked(customCurrency: CustomCurrency) {
        analyticsEventHandler.send(ManageTokens.CustomToken.TokenWasAdded(customCurrency))
    }
}