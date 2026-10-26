package com.tangem.tap.common.analytics.handlers.customerio

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Customer.io analytics handler.
 */
class CustomerIoAnalyticsHandler(
    private val client: CustomerIoAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler {

    override fun id(): String = ID

    override fun setUserId(userId: String) {
        client.setUserId(userId)
    }

    override fun clearUserId() {
        client.clearUserId()
    }

    override fun send(event: AnalyticsEvent) {
        // No-op: product events are not sent to Customer.io.
        // Triggers are configured to come from Amplitude directly.
    }

    companion object {
        const val ID = "CustomerIO"
    }
}