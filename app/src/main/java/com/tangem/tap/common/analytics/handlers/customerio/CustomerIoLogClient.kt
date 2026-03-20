package com.tangem.tap.common.analytics.handlers.customerio

import com.tangem.utils.logging.TangemLogger

/**
 * Log client for Customer.io (used in debug mode).
 *
 * Logs all operations to Timber instead of sending them to Customer.io.
 */
internal class CustomerIoLogClient : CustomerIoAnalyticsClient {

    private var userId: String? = null

    override fun setUserId(userId: String) {
        this.userId = userId
        TangemLogger.withTag(CustomerIoAnalyticsHandler.ID).d("identify: userId=$userId")
    }

    override fun clearUserId() {
        TangemLogger.withTag(CustomerIoAnalyticsHandler.ID).d("clearIdentify: previous userId=$userId")
        this.userId = null
    }
}