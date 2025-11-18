package com.tangem.features.send.v2.subcomponents.destination.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.VALIDATION
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

internal sealed class SendDestinationAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    abstract val categoryName: String

    /** Address to send entered */
    data class AddressEntered(
        override val categoryName: String,
        val method: EnterAddressSource,
        val isValid: Boolean,
        val source: CommonSendAnalyticEvents.CommonSendSource,
    ) : SendDestinationAnalyticEvents(
        category = categoryName,
        event = "Address Entered",
        params = mapOf(
            SOURCE to source.analyticsName,
            "Method" to method.name,
            VALIDATION to if (isValid) "Success" else "Fail",
        ),
    )

    /** Qr Code button clicked */
    data class QrCodeButtonClicked(
        override val categoryName: String,
    ) :
        SendDestinationAnalyticEvents(
            category = categoryName,
            event = "Button - QR Code",
        )
}