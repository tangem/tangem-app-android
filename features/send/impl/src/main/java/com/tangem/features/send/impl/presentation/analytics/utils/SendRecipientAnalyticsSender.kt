package com.tangem.features.send.impl.presentation.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
import com.tangem.features.send.impl.presentation.analytics.PasteType
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents

internal class SendRecipientAnalyticsSender(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun sendAddressAnalytics(type: EnterAddressSource?, isValidAddress: Boolean) {
        type?.let {
            if (type == EnterAddressSource.PasteButton) {
                analyticsEventHandler.send(SendAnalyticEvents.PasteButtonClicked(PasteType.Address))
            }
            analyticsEventHandler.send(SendAnalyticEvents.AddressEntered(it, isValidAddress))
        }
    }

    fun sendMemoAnalytics(isPasted: Boolean) {
        if (isPasted) {
            analyticsEventHandler.send(SendAnalyticEvents.PasteButtonClicked(PasteType.Memo))
        }
    }
}